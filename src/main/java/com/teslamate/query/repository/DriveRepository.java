package com.teslamate.query.repository;

import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.teslamate.query.repository.CarRepository.getDouble;
import static com.teslamate.query.repository.CarRepository.getInteger;
import static com.teslamate.query.repository.CarRepository.toInstant;

@Repository
public class DriveRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DriveRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long count(Long carId, Instant from, Instant to, Double minDistance, Integer minDuration,
                      Long geofenceId, String location, Boolean incompleteOnly) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM drives d
                LEFT JOIN addresses sa ON d.start_address_id = sa.id
                LEFT JOIN addresses ea ON d.end_address_id = ea.id
                LEFT JOIN geofences sg ON d.start_geofence_id = sg.id
                LEFT JOIN geofences eg ON d.end_geofence_id = eg.id
                WHERE 1=1
                """);
        MapSqlParameterSource params = baseFilters(sql, carId, from, to, minDistance, minDuration,
                geofenceId, location, incompleteOnly);
        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count == null ? 0 : count;
    }

    public List<DriveDto> find(Long carId, Instant from, Instant to, Double minDistance, Integer minDuration,
                               Long geofenceId, String location, Boolean incompleteOnly,
                               String rangeMode, int limit, int offset) {
        String startRange = "d.start_" + rangeMode + "_range_km";
        String endRange = "d.end_" + rangeMode + "_range_km";
        String sql = """
                SELECT
                  d.id, d.car_id, d.start_date, d.end_date, d.duration_min, d.distance,
                  d.outside_temp_avg, d.inside_temp_avg, d.speed_max, d.power_max, d.power_min,
                  d.ascent, d.descent, d.start_geofence_id, d.end_geofence_id,
                  %s AS start_range_km, %s AS end_range_km,
                  sp.battery_level AS start_battery_level,
                  ep.battery_level AS end_battery_level,
                  car.efficiency AS car_efficiency,
                  COALESCE(sg.name, CONCAT_WS(', ',
                    COALESCE(sa.name, NULLIF(CONCAT_WS(' ', sa.road, sa.house_number), '')), sa.city)) AS start_address,
                  COALESCE(eg.name, CONCAT_WS(', ',
                    COALESCE(ea.name, NULLIF(CONCAT_WS(' ', ea.road, ea.house_number), '')), ea.city)) AS end_address
                FROM drives d
                LEFT JOIN addresses sa ON d.start_address_id = sa.id
                LEFT JOIN addresses ea ON d.end_address_id = ea.id
                LEFT JOIN positions sp ON d.start_position_id = sp.id
                LEFT JOIN positions ep ON d.end_position_id = ep.id
                LEFT JOIN geofences sg ON d.start_geofence_id = sg.id
                LEFT JOIN geofences eg ON d.end_geofence_id = eg.id
                LEFT JOIN cars car ON car.id = d.car_id
                WHERE 1=1
                """.formatted(startRange, endRange);

        StringBuilder sb = new StringBuilder(sql);
        MapSqlParameterSource params = baseFilters(sb, carId, from, to, minDistance, minDuration,
                geofenceId, location, incompleteOnly);
        sb.append(" ORDER BY d.start_date DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", limit).addValue("offset", offset);

        return jdbc.query(sb.toString(), params, this::mapDrive);
    }

    public Optional<DriveDto> findById(long id, String rangeMode) {
        String startRange = "d.start_" + rangeMode + "_range_km";
        String endRange = "d.end_" + rangeMode + "_range_km";
        String sql = """
                SELECT
                  d.id, d.car_id, d.start_date, d.end_date, d.duration_min, d.distance,
                  d.outside_temp_avg, d.inside_temp_avg, d.speed_max, d.power_max, d.power_min,
                  d.ascent, d.descent, d.start_geofence_id, d.end_geofence_id,
                  %s AS start_range_km, %s AS end_range_km,
                  sp.battery_level AS start_battery_level,
                  ep.battery_level AS end_battery_level,
                  car.efficiency AS car_efficiency,
                  COALESCE(sg.name, CONCAT_WS(', ',
                    COALESCE(sa.name, NULLIF(CONCAT_WS(' ', sa.road, sa.house_number), '')), sa.city)) AS start_address,
                  COALESCE(eg.name, CONCAT_WS(', ',
                    COALESCE(ea.name, NULLIF(CONCAT_WS(' ', ea.road, ea.house_number), '')), ea.city)) AS end_address
                FROM drives d
                LEFT JOIN addresses sa ON d.start_address_id = sa.id
                LEFT JOIN addresses ea ON d.end_address_id = ea.id
                LEFT JOIN positions sp ON d.start_position_id = sp.id
                LEFT JOIN positions ep ON d.end_position_id = ep.id
                LEFT JOIN geofences sg ON d.start_geofence_id = sg.id
                LEFT JOIN geofences eg ON d.end_geofence_id = eg.id
                LEFT JOIN cars car ON car.id = d.car_id
                WHERE d.id = :id
                """.formatted(startRange, endRange);
        List<DriveDto> list = jdbc.query(sql, new MapSqlParameterSource("id", id), this::mapDrive);
        return list.stream().findFirst();
    }

    public List<DrivePositionDto> findPositions(long driveId, Integer downsampleSeconds) {
        // Optional simple downsample: take first row per time bucket
        String sql;
        if (downsampleSeconds != null && downsampleSeconds > 0) {
            sql = """
                    SELECT DISTINCT ON (bucket) id, date, latitude, longitude, elevation, speed, power,
                           odometer, ideal_battery_range_km, rated_battery_range_km,
                           battery_level, usable_battery_level, outside_temp, inside_temp
                    FROM (
                      SELECT *, floor(extract(epoch FROM date) / :bucket) AS bucket
                      FROM positions
                      WHERE drive_id = :driveId
                    ) t
                    ORDER BY bucket, date
                    """;
            return jdbc.query(sql, new MapSqlParameterSource()
                    .addValue("driveId", driveId)
                    .addValue("bucket", downsampleSeconds), this::mapPosition);
        }
        sql = """
                SELECT id, date, latitude, longitude, elevation, speed, power,
                       odometer, ideal_battery_range_km, rated_battery_range_km,
                       battery_level, usable_battery_level, outside_temp, inside_temp
                FROM positions
                WHERE drive_id = :driveId
                ORDER BY date
                """;
        return jdbc.query(sql, new MapSqlParameterSource("driveId", driveId), this::mapPosition);
    }

    private MapSqlParameterSource baseFilters(StringBuilder sql, Long carId, Instant from, Instant to,
                                              Double minDistance, Integer minDuration, Long geofenceId,
                                              String location, Boolean incompleteOnly) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (carId != null) {
            sql.append(" AND d.car_id = :carId");
            params.addValue("carId", carId);
        }
        if (from != null) {
            sql.append(" AND d.start_date >= :from");
            params.addValue("from", Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" AND d.start_date <= :to");
            params.addValue("to", Timestamp.from(to));
        }
        if (minDistance != null) {
            sql.append(" AND d.distance >= :minDistance");
            params.addValue("minDistance", minDistance);
        }
        if (minDuration != null) {
            sql.append(" AND d.duration_min >= :minDuration");
            params.addValue("minDuration", minDuration);
        }
        if (geofenceId != null) {
            sql.append(" AND (d.start_geofence_id = :geofenceId OR d.end_geofence_id = :geofenceId)");
            params.addValue("geofenceId", geofenceId);
        }
        if (location != null && !location.isBlank()) {
            sql.append("""
                     AND (
                       COALESCE(sg.name, '') ILIKE :loc OR COALESCE(eg.name, '') ILIKE :loc
                       OR COALESCE(sa.city, '') ILIKE :loc OR COALESCE(ea.city, '') ILIKE :loc
                       OR COALESCE(sa.name, '') ILIKE :loc OR COALESCE(ea.name, '') ILIKE :loc
                       OR COALESCE(sa.road, '') ILIKE :loc OR COALESCE(ea.road, '') ILIKE :loc
                     )
                    """);
            params.addValue("loc", "%" + location + "%");
        }
        if (Boolean.TRUE.equals(incompleteOnly)) {
            sql.append(" AND d.end_date IS NULL");
        }
        return params;
    }

    private DriveDto mapDrive(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal startRange = rs.getBigDecimal("start_range_km");
        BigDecimal endRange = rs.getBigDecimal("end_range_km");
        BigDecimal rangeDiff = null;
        if (startRange != null && endRange != null) {
            rangeDiff = startRange.subtract(endRange);
        }
        Double efficiency = getDouble(rs, "car_efficiency");
        Double consumptionKwh = null;
        Double consumptionPerKm = null;
        Double distance = getDouble(rs, "distance");
        if (rangeDiff != null && efficiency != null) {
            consumptionKwh = rangeDiff.doubleValue() * efficiency;
            if (distance != null && distance > 0) {
                consumptionPerKm = consumptionKwh / distance;
            }
        }
        Integer durationMin = getInteger(rs, "duration_min");
        Double avgSpeed = null;
        Instant start = toInstant(rs.getTimestamp("start_date"));
        Instant end = toInstant(rs.getTimestamp("end_date"));
        if (distance != null) {
            if (durationMin != null && durationMin > 0) {
                avgSpeed = distance / (durationMin * 60.0) * 3600.0;
            } else if (start != null && end != null && end.isAfter(start)) {
                double seconds = (end.toEpochMilli() - start.toEpochMilli()) / 1000.0;
                if (seconds > 0) {
                    avgSpeed = distance / seconds * 3600.0;
                }
            }
        }
        if (avgSpeed != null) {
            avgSpeed = BigDecimal.valueOf(avgSpeed).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
        if (consumptionKwh != null) {
            consumptionKwh = BigDecimal.valueOf(consumptionKwh).setScale(3, RoundingMode.HALF_UP).doubleValue();
        }
        if (consumptionPerKm != null) {
            consumptionPerKm = BigDecimal.valueOf(consumptionPerKm).setScale(5, RoundingMode.HALF_UP).doubleValue();
        }

        return new DriveDto(
                rs.getLong("id"),
                rs.getLong("car_id"),
                start,
                end,
                rs.getString("start_address"),
                rs.getString("end_address"),
                durationMin,
                distance,
                getInteger(rs, "start_battery_level"),
                getInteger(rs, "end_battery_level"),
                startRange,
                endRange,
                rangeDiff,
                consumptionKwh,
                consumptionPerKm,
                getDouble(rs, "outside_temp_avg"),
                getDouble(rs, "inside_temp_avg"),
                avgSpeed,
                getInteger(rs, "speed_max"),
                getInteger(rs, "power_max"),
                getInteger(rs, "power_min"),
                getInteger(rs, "ascent"),
                getInteger(rs, "descent"),
                efficiency,
                getLong(rs, "start_geofence_id"),
                getLong(rs, "end_geofence_id")
        );
    }

    private DrivePositionDto mapPosition(ResultSet rs, int rowNum) throws SQLException {
        return new DrivePositionDto(
                rs.getLong("id"),
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

    private static Long getLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }
}
