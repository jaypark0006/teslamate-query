package com.teslamate.query.repository;

import com.teslamate.query.db.JdbiRepository;
import com.teslamate.query.dto.BatteryHealthDto;
import com.teslamate.query.dto.LocationStatsDto;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.dto.ProjectedRangeDto;
import com.teslamate.query.dto.StateDto;
import com.teslamate.query.dto.TimelineEventDto;
import com.teslamate.query.dto.UpdateDto;
import com.teslamate.query.dto.VampireDrainDto;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class AdvancedRepository extends JdbiRepository {

    public AdvancedRepository(Jdbi jdbi) {
        super(jdbi);
    }

    public List<StateDto> states(long carId, Instant from, Instant to) {
        return queryList("""
                SELECT id, car_id, state::text AS state, start_date, end_date,
                       CASE WHEN end_date IS NOT NULL
                            THEN extract(epoch FROM (end_date - start_date))::bigint
                            ELSE NULL END AS duration_seconds
                FROM states
                WHERE car_id = :carId
                  AND start_date <= :to
                  AND (end_date IS NULL OR end_date >= :from)
                ORDER BY start_date
                """, StateDto.class, "carId", carId, "from", from, "to", to);
    }

    public List<UpdateDto> updates(long carId, Instant from, Instant to) {
        return queryList("""
                SELECT id, car_id, start_date, end_date, version
                FROM updates
                WHERE car_id = :carId
                  AND start_date >= :from AND start_date <= :to
                ORDER BY start_date DESC
                """, UpdateDto.class, "carId", carId, "from", from, "to", to);
    }

    public List<TimelineEventDto> timeline(long carId, Instant from, Instant to) {
        return queryList("""
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
                """, TimelineEventDto.class, "carId", carId, "from", from, "to", to);
    }

    public List<VampireDrainDto.Segment> vampireSegments(long carId, Instant from, Instant to, String rangeMode) {
        String sql = """
                WITH ordered AS (
                  SELECT
                    d.id,
                    d.end_date AS start_date,
                    lead(d.start_date) OVER (ORDER BY d.start_date) AS end_date,
                    d.end_%1$s_range_km AS start_range_km,
                    lead(d.start_%1$s_range_km) OVER (ORDER BY d.start_date) AS end_range_km,
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
                  start_date, end_date, start_range_km, end_range_km, start_address, end_address,
                  extract(epoch FROM (end_date - start_date)) / 3600.0 AS hours,
                  (start_range_km - end_range_km) AS range_loss_km,
                  CASE WHEN extract(epoch FROM (end_date - start_date)) > 0
                       THEN (start_range_km - end_range_km)
                            / (extract(epoch FROM (end_date - start_date)) / 3600.0)
                       ELSE NULL END AS loss_per_hour_km
                FROM ordered
                WHERE end_date IS NOT NULL
                  AND end_date > start_date
                  AND start_range_km IS NOT NULL AND end_range_km IS NOT NULL
                  AND start_range_km >= end_range_km
                ORDER BY start_date
                """.formatted(rangeMode);
        return queryList(sql, VampireDrainDto.Segment.class, "carId", carId, "from", from, "to", to);
    }

    public List<ProjectedRangeDto.Point> projectedRange(long carId, Instant from, Instant to, String rangeMode) {
        String sql = """
                SELECT date, odometer AS odometer_km, battery_level,
                       outside_temp AS outside_temp_c,
                       CASE WHEN battery_level IS NOT NULL AND battery_level > 0
                            THEN %1$s_battery_range_km * 100.0 / battery_level
                            ELSE NULL END AS projected_full_range_km
                FROM positions
                WHERE car_id = :carId
                  AND date >= :from AND date <= :to
                  AND ideal_battery_range_km IS NOT NULL
                  AND battery_level IS NOT NULL AND battery_level > 5
                ORDER BY date
                """.formatted(rangeMode);
        return queryList(sql, ProjectedRangeDto.Point.class, "carId", carId, "from", from, "to", to);
    }

    public List<BatteryHealthDto.CapacityPoint> batteryCapacity(long carId, Instant from, Instant to, String rangeMode) {
        String sql = """
                SELECT
                  cp.end_date AS date,
                  p.odometer AS odometer_km,
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
        return queryList(sql, BatteryHealthDto.CapacityPoint.class, "carId", carId, "from", from, "to", to);
    }

    public List<LocationStatsDto.Place> locationStats(long carId, Instant from, Instant to) {
        return queryList("""
                SELECT name, city, country, kind,
                       sum(cnt) AS visit_count,
                       sum(distance) AS total_distance_km
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
                """, LocationStatsDto.Place.class, "carId", carId, "from", from, "to", to);
    }

    public long countPositions(long carId, Instant from, Instant to, boolean cleanOnly) {
        String clean = cleanOnly ? " AND ideal_battery_range_km IS NOT NULL" : "";
        return queryLong("""
                SELECT count(*) FROM positions
                WHERE car_id = :carId AND date >= :from AND date <= :to
                """ + clean, "carId", carId, "from", from, "to", to);
    }

    public List<PositionDto> positions(long carId, Instant from, Instant to, boolean cleanOnly,
                                       Integer downsampleSeconds, int limit, int offset) {
        String clean = cleanOnly ? " AND ideal_battery_range_km IS NOT NULL" : "";
        if (downsampleSeconds != null && downsampleSeconds > 0) {
            return queryList("""
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
                    """.formatted(clean), PositionDto.class,
                    "carId", carId, "from", from, "to", to,
                    "bucket", downsampleSeconds, "limit", limit, "offset", offset);
        }
        return queryList("""
                SELECT id, car_id, drive_id, date, latitude, longitude, elevation, speed, power,
                       odometer, ideal_battery_range_km, rated_battery_range_km,
                       battery_level, usable_battery_level, outside_temp, inside_temp
                FROM positions
                WHERE car_id = :carId AND date >= :from AND date <= :to
                %s
                ORDER BY date
                LIMIT :limit OFFSET :offset
                """.formatted(clean), PositionDto.class,
                "carId", carId, "from", from, "to", to, "limit", limit, "offset", offset);
    }
}
