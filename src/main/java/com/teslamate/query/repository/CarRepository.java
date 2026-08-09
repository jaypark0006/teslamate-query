package com.teslamate.query.repository;

import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class CarRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CarRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CarDto> findAll() {
        String sql = """
                SELECT c.id, c.name, c.vin, c.model, c.marketing_name, c.trim_badging,
                       c.efficiency, c.display_priority, c.exterior_color, c.wheel_type,
                       cs.lfp_battery, cs.free_supercharging, cs.enabled
                FROM cars c
                LEFT JOIN car_settings cs ON c.settings_id = cs.id
                ORDER BY c.display_priority NULLS LAST, c.name NULLS LAST, c.vin
                """;
        return jdbc.query(sql, this::mapCar);
    }

    public Optional<CarDto> findById(long id) {
        String sql = """
                SELECT c.id, c.name, c.vin, c.model, c.marketing_name, c.trim_badging,
                       c.efficiency, c.display_priority, c.exterior_color, c.wheel_type,
                       cs.lfp_battery, cs.free_supercharging, cs.enabled
                FROM cars c
                LEFT JOIN car_settings cs ON c.settings_id = cs.id
                WHERE c.id = :id
                """;
        List<CarDto> list = jdbc.query(sql, new MapSqlParameterSource("id", id), this::mapCar);
        return list.stream().findFirst();
    }

    public Optional<LatestSnapshotDto> findLatest(long carId) {
        String sql = """
                SELECT * FROM (
                  (SELECT p.date, 'position' AS source, p.battery_level, p.usable_battery_level,
                          p.ideal_battery_range_km, p.rated_battery_range_km, p.odometer,
                          p.latitude, p.longitude, p.outside_temp, p.inside_temp,
                          p.speed, p.power, NULL::int AS charger_power, NULL::int AS charger_voltage
                   FROM positions p
                   WHERE p.car_id = :carId AND p.ideal_battery_range_km IS NOT NULL
                   ORDER BY p.date DESC
                   LIMIT 1)
                  UNION ALL
                  (SELECT c.date, 'charge' AS source, c.battery_level, c.usable_battery_level,
                          c.ideal_battery_range_km, c.rated_battery_range_km, pos.odometer,
                          pos.latitude, pos.longitude, c.outside_temp, NULL::numeric AS inside_temp,
                          NULL::int AS speed, NULL::int AS power, c.charger_power, c.charger_voltage
                   FROM charges c
                   JOIN charging_processes cp ON cp.id = c.charging_process_id
                   LEFT JOIN positions pos ON pos.id = cp.position_id
                   WHERE cp.car_id = :carId
                   ORDER BY c.date DESC
                   LIMIT 1)
                ) t
                ORDER BY date DESC
                LIMIT 1
                """;
        List<LatestSnapshotDto> list = jdbc.query(sql, new MapSqlParameterSource("carId", carId), (rs, i) ->
                new LatestSnapshotDto(
                        carId,
                        toInstant(rs.getTimestamp("date")),
                        rs.getString("source"),
                        getInteger(rs, "battery_level"),
                        getInteger(rs, "usable_battery_level"),
                        rs.getBigDecimal("ideal_battery_range_km"),
                        rs.getBigDecimal("rated_battery_range_km"),
                        getDouble(rs, "odometer"),
                        rs.getBigDecimal("latitude"),
                        rs.getBigDecimal("longitude"),
                        rs.getBigDecimal("outside_temp"),
                        rs.getBigDecimal("inside_temp"),
                        getInteger(rs, "speed"),
                        getInteger(rs, "power"),
                        getInteger(rs, "charger_power"),
                        getInteger(rs, "charger_voltage")
                ));
        return list.stream().findFirst();
    }

    private CarDto mapCar(ResultSet rs, int rowNum) throws SQLException {
        return new CarDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("vin"),
                rs.getString("model"),
                rs.getString("marketing_name"),
                rs.getString("trim_badging"),
                getDouble(rs, "efficiency"),
                getInteger(rs, "display_priority"),
                rs.getString("exterior_color"),
                rs.getString("wheel_type"),
                getBoolean(rs, "lfp_battery"),
                getBoolean(rs, "free_supercharging"),
                getBoolean(rs, "enabled")
        );
    }

    static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    static Integer getInteger(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    static Double getDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    static Boolean getBoolean(ResultSet rs, String col) throws SQLException {
        boolean v = rs.getBoolean(col);
        return rs.wasNull() ? null : v;
    }
}
