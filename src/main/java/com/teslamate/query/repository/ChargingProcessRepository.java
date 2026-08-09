package com.teslamate.query.repository;

import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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
public class ChargingProcessRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ChargingProcessRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long count(Long carId, Instant from, Instant to, Long geofenceId, String chargeType, Boolean incompleteOnly) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM (
                  SELECT cp.id,
                    CASE WHEN NULLIF(mode() WITHIN GROUP (ORDER BY c.charger_phases), 0) IS NULL
                         THEN 'DC' ELSE 'AC' END AS charge_type
                  FROM charging_processes cp
                  LEFT JOIN charges c ON c.charging_process_id = cp.id
                  WHERE (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                """);
        MapSqlParameterSource params = baseFilters(sql, carId, from, to, geofenceId, incompleteOnly, true);
        sql.append(" GROUP BY cp.id");
        sql.append(") t WHERE 1=1");
        if (chargeType != null && !chargeType.isBlank()) {
            sql.append(" AND t.charge_type = :chargeType");
            params.addValue("chargeType", chargeType.toUpperCase());
        }
        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count == null ? 0 : count;
    }

    public List<ChargingProcessDto> find(Long carId, Instant from, Instant to, Long geofenceId,
                                         String chargeType, Boolean incompleteOnly, String rangeMode,
                                         int limit, int offset) {
        String rangeAdded = "cp.end_" + rangeMode + "_range_km - cp.start_" + rangeMode + "_range_km";
        StringBuilder sql = new StringBuilder("""
                SELECT * FROM (
                  SELECT
                    cp.id, cp.car_id, cp.start_date, cp.end_date,
                    cp.charge_energy_added, cp.charge_energy_used, cp.duration_min,
                    cp.start_battery_level, cp.end_battery_level, cp.outside_temp_avg, cp.cost,
                    cp.geofence_id,
                    %s AS range_added_km,
                    CONCAT_WS(', ',
                      COALESCE(a.name, NULLIF(CONCAT_WS(' ', a.road, a.house_number), '')), a.city) AS address,
                    g.name AS geofence_name,
                    p.latitude, p.longitude, p.odometer,
                    cars.efficiency,
                    max(c.charger_voltage) AS max_charger_voltage,
                    CASE WHEN NULLIF(mode() WITHIN GROUP (ORDER BY c.charger_phases), 0) IS NULL
                         THEN 'DC' ELSE 'AC' END AS charge_type
                  FROM charging_processes cp
                  LEFT JOIN charges c ON c.charging_process_id = cp.id
                  LEFT JOIN positions p ON p.id = cp.position_id
                  LEFT JOIN cars ON cars.id = cp.car_id
                  LEFT JOIN addresses a ON a.id = cp.address_id
                  LEFT JOIN geofences g ON g.id = cp.geofence_id
                  WHERE (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                """.formatted(rangeAdded));
        MapSqlParameterSource params = baseFilters(sql, carId, from, to, geofenceId, incompleteOnly, false);
        sql.append("""
                  GROUP BY cp.id, cp.car_id, cp.start_date, cp.end_date, cp.charge_energy_added,
                           cp.charge_energy_used, cp.duration_min, cp.start_battery_level, cp.end_battery_level,
                           cp.outside_temp_avg, cp.cost, cp.geofence_id, range_added_km, address,
                           g.name, p.latitude, p.longitude, p.odometer, cars.efficiency
                ) t WHERE 1=1
                """);
        if (chargeType != null && !chargeType.isBlank()) {
            sql.append(" AND t.charge_type = :chargeType");
            params.addValue("chargeType", chargeType.toUpperCase());
        }
        sql.append(" ORDER BY t.start_date DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", limit).addValue("offset", offset);
        return jdbc.query(sql.toString(), params, this::mapProcess);
    }

    public Optional<ChargingProcessDto> findById(long id, String rangeMode) {
        String rangeAdded = "cp.end_" + rangeMode + "_range_km - cp.start_" + rangeMode + "_range_km";
        String sql = """
                SELECT
                  cp.id, cp.car_id, cp.start_date, cp.end_date,
                  cp.charge_energy_added, cp.charge_energy_used, cp.duration_min,
                  cp.start_battery_level, cp.end_battery_level, cp.outside_temp_avg, cp.cost,
                  cp.geofence_id,
                  %s AS range_added_km,
                  CONCAT_WS(', ',
                    COALESCE(a.name, NULLIF(CONCAT_WS(' ', a.road, a.house_number), '')), a.city) AS address,
                  g.name AS geofence_name,
                  p.latitude, p.longitude, p.odometer,
                  cars.efficiency,
                  max(c.charger_voltage) AS max_charger_voltage,
                  CASE WHEN NULLIF(mode() WITHIN GROUP (ORDER BY c.charger_phases), 0) IS NULL
                       THEN 'DC' ELSE 'AC' END AS charge_type
                FROM charging_processes cp
                LEFT JOIN charges c ON c.charging_process_id = cp.id
                LEFT JOIN positions p ON p.id = cp.position_id
                LEFT JOIN cars ON cars.id = cp.car_id
                LEFT JOIN addresses a ON a.id = cp.address_id
                LEFT JOIN geofences g ON g.id = cp.geofence_id
                WHERE cp.id = :id
                GROUP BY cp.id, cp.car_id, cp.start_date, cp.end_date, cp.charge_energy_added,
                         cp.charge_energy_used, cp.duration_min, cp.start_battery_level, cp.end_battery_level,
                         cp.outside_temp_avg, cp.cost, cp.geofence_id, range_added_km, address,
                         g.name, p.latitude, p.longitude, p.odometer, cars.efficiency
                """.formatted(rangeAdded);
        List<ChargingProcessDto> list = jdbc.query(sql, new MapSqlParameterSource("id", id), this::mapProcess);
        return list.stream().findFirst();
    }

    public List<ChargeSampleDto> findSamples(long processId) {
        String sql = """
                SELECT id, date, battery_level, usable_battery_level, charge_energy_added,
                       charger_power, charger_voltage, charger_actual_current, charger_phases,
                       fast_charger_present, fast_charger_type,
                       ideal_battery_range_km, rated_battery_range_km, outside_temp, battery_heater_on
                FROM charges
                WHERE charging_process_id = :id
                ORDER BY date
                """;
        return jdbc.query(sql, new MapSqlParameterSource("id", processId), (rs, i) -> new ChargeSampleDto(
                rs.getLong("id"),
                toInstant(rs.getTimestamp("date")),
                getInteger(rs, "battery_level"),
                getInteger(rs, "usable_battery_level"),
                rs.getBigDecimal("charge_energy_added"),
                getInteger(rs, "charger_power"),
                getInteger(rs, "charger_voltage"),
                getInteger(rs, "charger_actual_current"),
                getInteger(rs, "charger_phases"),
                (Boolean) rs.getObject("fast_charger_present"),
                rs.getString("fast_charger_type"),
                rs.getBigDecimal("ideal_battery_range_km"),
                rs.getBigDecimal("rated_battery_range_km"),
                rs.getBigDecimal("outside_temp"),
                (Boolean) rs.getObject("battery_heater_on")
        ));
    }

    private MapSqlParameterSource baseFilters(StringBuilder sql, Long carId, Instant from, Instant to,
                                              Long geofenceId, Boolean incompleteOnly, boolean forCount) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (carId != null) {
            sql.append(" AND cp.car_id = :carId");
            params.addValue("carId", carId);
        }
        if (from != null) {
            sql.append(" AND cp.start_date >= :from");
            params.addValue("from", Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" AND cp.start_date <= :to");
            params.addValue("to", Timestamp.from(to));
        }
        if (geofenceId != null) {
            sql.append(" AND cp.geofence_id = :geofenceId");
            params.addValue("geofenceId", geofenceId);
        }
        if (Boolean.TRUE.equals(incompleteOnly)) {
            sql.append(" AND cp.end_date IS NULL");
        }
        return params;
    }

    private ChargingProcessDto mapProcess(ResultSet rs, int rowNum) throws SQLException {
        Long geofenceId = rs.getLong("geofence_id");
        if (rs.wasNull()) {
            geofenceId = null;
        }
        return new ChargingProcessDto(
                rs.getLong("id"),
                rs.getLong("car_id"),
                toInstant(rs.getTimestamp("start_date")),
                toInstant(rs.getTimestamp("end_date")),
                rs.getString("address"),
                rs.getString("geofence_name"),
                geofenceId,
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getBigDecimal("charge_energy_added"),
                rs.getBigDecimal("charge_energy_used"),
                getInteger(rs, "duration_min"),
                getInteger(rs, "start_battery_level"),
                getInteger(rs, "end_battery_level"),
                rs.getBigDecimal("range_added_km"),
                rs.getBigDecimal("outside_temp_avg"),
                rs.getBigDecimal("cost"),
                rs.getString("charge_type"),
                getInteger(rs, "max_charger_voltage"),
                getDouble(rs, "odometer"),
                getDouble(rs, "efficiency")
        );
    }
}
