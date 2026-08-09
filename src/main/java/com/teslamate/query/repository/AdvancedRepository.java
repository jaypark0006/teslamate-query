package com.teslamate.query.repository;

import com.teslamate.query.dto.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static com.teslamate.query.repository.CarRepository.*;

@Repository
public class AdvancedRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AdvancedRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private MapSqlParameterSource carTime(long carId, Instant from, Instant to) {
        return new MapSqlParameterSource()
                .addValue("carId", carId)
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));
    }

    public List<StateDto> states(long carId, Instant from, Instant to) {
        String sql = """
                SELECT id, car_id, state::text AS state, start_date, end_date,
                       CASE WHEN end_date IS NOT NULL
                            THEN extract(epoch FROM (end_date - start_date))::bigint
                            ELSE NULL END AS duration_seconds
                FROM states
                WHERE car_id = :carId
                  AND start_date <= :to
                  AND (end_date IS NULL OR end_date >= :from)
                ORDER BY start_date
                """;
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new StateDto(
                rs.getLong("id"),
                rs.getLong("car_id"),
                rs.getString("state"),
                toInstant(rs.getTimestamp("start_date")),
                toInstant(rs.getTimestamp("end_date")),
                getLong(rs, "duration_seconds")
        ));
    }

    public List<UpdateDto> updates(long carId, Instant from, Instant to) {
        String sql = """
                SELECT id, car_id, start_date, end_date, version
                FROM updates
                WHERE car_id = :carId
                  AND start_date >= :from AND start_date <= :to
                ORDER BY start_date DESC
                """;
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new UpdateDto(
                rs.getLong("id"),
                rs.getLong("car_id"),
                toInstant(rs.getTimestamp("start_date")),
                toInstant(rs.getTimestamp("end_date")),
                rs.getString("version")
        ));
    }

    public List<TimelineEventDto> timeline(long carId, Instant from, Instant to) {
        String sql = """
                SELECT * FROM (
                  SELECT 'drive' AS type, id AS ref_id, start_date, end_date,
                         concat('Drive #', id) AS label, distance AS value
                  FROM drives
                  WHERE car_id = :carId AND start_date <= :to
                    AND (end_date IS NULL OR end_date >= :from)
                  UNION ALL
                  SELECT 'charge', id, start_date, end_date,
                         concat('Charge #', id), charge_energy_added
                  FROM charging_processes
                  WHERE car_id = :carId AND start_date <= :to
                    AND (end_date IS NULL OR end_date >= :from)
                  UNION ALL
                  SELECT 'state', id, start_date, end_date, state::text, NULL
                  FROM states
                  WHERE car_id = :carId AND start_date <= :to
                    AND (end_date IS NULL OR end_date >= :from)
                  UNION ALL
                  SELECT 'update', id, start_date, end_date, version, NULL
                  FROM updates
                  WHERE car_id = :carId AND start_date <= :to
                    AND (end_date IS NULL OR end_date >= :from)
                ) t
                ORDER BY start_date
                """;
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new TimelineEventDto(
                rs.getString("type"),
                rs.getLong("ref_id"),
                toInstant(rs.getTimestamp("start_date")),
                toInstant(rs.getTimestamp("end_date")),
                rs.getString("label"),
                getDouble(rs, "value")
        ));
    }

    public List<VampireDrainDto.Segment> vampireSegments(long carId, Instant from, Instant to, String rangeMode) {
        // Parked gaps between consecutive completed drives: range loss while not driving
        String sql = """
                WITH ordered AS (
                  SELECT
                    d.id,
                    d.end_date AS park_start,
                    lead(d.start_date) OVER (ORDER BY d.start_date) AS park_end,
                    d.end_%1$s_range_km AS start_range,
                    lead(d.start_%1$s_range_km) OVER (ORDER BY d.start_date) AS end_range,
                    COALESCE(eg.name, CONCAT_WS(', ',
                      COALESCE(ea.name, NULLIF(CONCAT_WS(' ', ea.road, ea.house_number), '')), ea.city)) AS start_address,
                    lead(COALESCE(sg.name, CONCAT_WS(', ',
                      COALESCE(sa.name, NULLIF(CONCAT_WS(' ', sa.road, sa.house_number), '')), sa.city)))
                      OVER (ORDER BY d.start_date) AS end_address
                  FROM drives d
                  LEFT JOIN addresses ea ON d.end_address_id = ea.id
                  LEFT JOIN addresses sa ON d.start_address_id = sa.id
                  LEFT JOIN geofences eg ON d.end_geofence_id = eg.id
                  LEFT JOIN geofences sg ON d.start_geofence_id = sg.id
                  WHERE d.car_id = :carId
                    AND d.end_date IS NOT NULL
                    AND d.start_date >= :from AND d.start_date <= :to
                )
                SELECT
                  park_start, park_end, start_range, end_range, start_address, end_address,
                  extract(epoch FROM (park_end - park_start)) / 3600.0 AS hours,
                  (start_range - end_range) AS range_loss
                FROM ordered
                WHERE park_end IS NOT NULL
                  AND park_end > park_start
                  AND start_range IS NOT NULL AND end_range IS NOT NULL
                  AND start_range >= end_range
                ORDER BY park_start
                """.formatted(rangeMode);
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> {
            Double hours = getDouble(rs, "hours");
            Double loss = getDouble(rs, "range_loss");
            Double perHour = (hours != null && hours > 0 && loss != null) ? loss / hours : null;
            return new VampireDrainDto.Segment(
                    toInstant(rs.getTimestamp("park_start")),
                    toInstant(rs.getTimestamp("park_end")),
                    hours,
                    getDouble(rs, "start_range"),
                    getDouble(rs, "end_range"),
                    loss,
                    perHour,
                    rs.getString("start_address"),
                    rs.getString("end_address")
            );
        });
    }

    public List<ProjectedRangeDto.Point> projectedRange(long carId, Instant from, Instant to, String rangeMode) {
        String sql = """
                SELECT date, odometer, battery_level, outside_temp,
                       CASE WHEN battery_level IS NOT NULL AND battery_level > 0
                            THEN %1$s_battery_range_km * 100.0 / battery_level
                            ELSE NULL END AS projected_full_range
                FROM positions
                WHERE car_id = :carId
                  AND date >= :from AND date <= :to
                  AND ideal_battery_range_km IS NOT NULL
                  AND battery_level IS NOT NULL AND battery_level > 5
                ORDER BY date
                """.formatted(rangeMode);
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new ProjectedRangeDto.Point(
                toInstant(rs.getTimestamp("date")),
                getDouble(rs, "odometer"),
                getDouble(rs, "projected_full_range"),
                getInteger(rs, "battery_level"),
                getDouble(rs, "outside_temp")
        ));
    }

    public List<BatteryHealthDto.CapacityPoint> batteryCapacity(long carId, Instant from, Instant to, String rangeMode) {
        // Capacity estimate from completed charges: energy_added / soc_delta * 100, with full range proxy
        String sql = """
                SELECT
                  cp.end_date AS date,
                  p.odometer AS odometer,
                  CASE WHEN (cp.end_battery_level - cp.start_battery_level) > 0
                       THEN cp.charge_energy_added * 100.0 / (cp.end_battery_level - cp.start_battery_level)
                       ELSE NULL END AS capacity_kwh,
                  CASE WHEN cp.end_battery_level > 0
                       THEN cp.end_%1$s_range_km * 100.0 / cp.end_battery_level
                       ELSE NULL END AS full_range_km
                FROM charging_processes cp
                LEFT JOIN positions p ON p.id = cp.position_id
                WHERE cp.car_id = :carId
                  AND cp.end_date IS NOT NULL
                  AND cp.start_date >= :from AND cp.start_date <= :to
                  AND cp.charge_energy_added > 1
                  AND cp.end_battery_level > cp.start_battery_level
                ORDER BY cp.end_date
                """.formatted(rangeMode);
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new BatteryHealthDto.CapacityPoint(
                toInstant(rs.getTimestamp("date")),
                getDouble(rs, "odometer"),
                getDouble(rs, "capacity_kwh"),
                getDouble(rs, "full_range_km")
        ));
    }

    public List<LocationStatsDto.Place> locationStats(long carId, Instant from, Instant to) {
        String sql = """
                SELECT name, city, country, kind, sum(cnt) AS visit_count, sum(distance) AS total_distance
                FROM (
                  SELECT
                    coalesce(sg.name, sa.name, sa.road, 'Unknown') AS name,
                    sa.city, sa.country, 'drive_start' AS kind,
                    count(*) AS cnt, coalesce(sum(d.distance), 0) AS distance
                  FROM drives d
                  LEFT JOIN addresses sa ON d.start_address_id = sa.id
                  LEFT JOIN geofences sg ON d.start_geofence_id = sg.id
                  WHERE d.car_id = :carId AND d.start_date >= :from AND d.start_date <= :to
                  GROUP BY 1, 2, 3, 4
                  UNION ALL
                  SELECT
                    coalesce(g.name, a.name, a.road, 'Unknown'),
                    a.city, a.country, 'charge',
                    count(*), 0
                  FROM charging_processes cp
                  LEFT JOIN addresses a ON cp.address_id = a.id
                  LEFT JOIN geofences g ON cp.geofence_id = g.id
                  WHERE cp.car_id = :carId AND cp.start_date >= :from AND cp.start_date <= :to
                  GROUP BY 1, 2, 3, 4
                ) t
                GROUP BY name, city, country, kind
                ORDER BY visit_count DESC
                LIMIT 100
                """;
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new LocationStatsDto.Place(
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getLong("visit_count"),
                rs.getDouble("total_distance"),
                rs.getString("kind")
        ));
    }

    public long countPositions(long carId, Instant from, Instant to, boolean cleanOnly) {
        String sql = """
                SELECT count(*) FROM positions
                WHERE car_id = :carId AND date >= :from AND date <= :to
                """ + (cleanOnly ? " AND ideal_battery_range_km IS NOT NULL" : "");
        Long v = jdbc.queryForObject(sql, carTime(carId, from, to), Long.class);
        return v == null ? 0 : v;
    }

    public List<PositionDto> positions(long carId, Instant from, Instant to, boolean cleanOnly,
                                       Integer downsampleSeconds, int limit, int offset) {
        MapSqlParameterSource params = carTime(carId, from, to)
                .addValue("limit", limit)
                .addValue("offset", offset);
        String clean = cleanOnly ? " AND ideal_battery_range_km IS NOT NULL" : "";
        if (downsampleSeconds != null && downsampleSeconds > 0) {
            String sql = """
                    SELECT id, car_id, drive_id, date, latitude, longitude, elevation, speed, power,
                           odometer, ideal_battery_range_km, rated_battery_range_km,
                           battery_level, usable_battery_level, outside_temp, inside_temp
                    FROM (
                      SELECT DISTINCT ON (bucket) *,
                             floor(extract(epoch FROM date) / :bucket) AS bucket
                      FROM positions
                      WHERE car_id = :carId AND date >= :from AND date <= :to
                      %s
                      ORDER BY bucket, date
                    ) t
                    ORDER BY date
                    LIMIT :limit OFFSET :offset
                    """.formatted(clean);
            params.addValue("bucket", downsampleSeconds);
            return jdbc.query(sql, params, this::mapPosition);
        }
        String sql = """
                SELECT id, car_id, drive_id, date, latitude, longitude, elevation, speed, power,
                       odometer, ideal_battery_range_km, rated_battery_range_km,
                       battery_level, usable_battery_level, outside_temp, inside_temp
                FROM positions
                WHERE car_id = :carId AND date >= :from AND date <= :to
                %s
                ORDER BY date
                LIMIT :limit OFFSET :offset
                """.formatted(clean);
        return jdbc.query(sql, params, this::mapPosition);
    }

    private PositionDto mapPosition(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        Long driveId = rs.getLong("drive_id");
        if (rs.wasNull()) {
            driveId = null;
        }
        return new PositionDto(
                rs.getLong("id"),
                rs.getLong("car_id"),
                driveId,
                toInstant(rs.getTimestamp("date")),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                getInteger(rs, "elevation"),
                getInteger(rs, "speed"),
                getInteger(rs, "power"),
                getDouble(rs, "odometer"),
                rs.getBigDecimal("ideal_battery_range_km"),
                rs.getBigDecimal("rated_battery_range_km"),
                getInteger(rs, "battery_level"),
                getInteger(rs, "usable_battery_level"),
                rs.getBigDecimal("outside_temp"),
                rs.getBigDecimal("inside_temp")
        );
    }

    private static Long getLong(java.sql.ResultSet rs, String col) throws java.sql.SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }
}
