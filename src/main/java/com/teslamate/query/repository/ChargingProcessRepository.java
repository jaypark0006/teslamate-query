package com.teslamate.query.repository;

import com.teslamate.query.db.JdbiRepository;
import com.teslamate.query.db.SqlQueryBuilder;
import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.ChargingProcessEnrichedDto;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ChargingProcessRepository extends JdbiRepository {

    private static final String PROCESS_SELECT = """
            SELECT
              cp.id,
              cp.car_id,
              cp.start_date,
              cp.end_date,
              cp.charge_energy_added,
              cp.charge_energy_used,
              cp.duration_min,
              cp.start_battery_level,
              cp.end_battery_level,
              cp.start_ideal_range_km,
              cp.end_ideal_range_km,
              cp.start_rated_range_km,
              cp.end_rated_range_km,
              cp.outside_temp_avg AS outside_temp_avg_c,
              cp.cost,
              cp.position_id,
              cp.address_id,
              cp.geofence_id
            FROM charging_processes cp
            """;

    public ChargingProcessRepository(Jdbi jdbi) {
        super(jdbi);
    }

    public long count(Long carId, Instant from, Instant to, Long geofenceId, String chargeType, Boolean incompleteOnly) {
        if (chargeType != null && !chargeType.isBlank()) {
            SqlQueryBuilder q = SqlQueryBuilder.of("""
                    SELECT COUNT(*) FROM (
                      SELECT cp.id,
                        CASE WHEN NULLIF(mode() WITHIN GROUP (ORDER BY c.charger_phases), 0) IS NULL
                             THEN 'DC' ELSE 'AC' END AS charge_type
                      FROM charging_processes cp
                      LEFT JOIN charges c ON c.charging_process_id = cp.id
                      WHERE (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                    """);
            applyFilters(q, carId, from, to, geofenceId, incompleteOnly);
            q.append(" GROUP BY cp.id) t WHERE t.charge_type = :chargeType")
                    .bind("chargeType", chargeType.toUpperCase());
            return longValue(q);
        }
        SqlQueryBuilder q = SqlQueryBuilder.of("""
                SELECT COUNT(*) FROM charging_processes cp
                WHERE (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                """);
        applyFilters(q, carId, from, to, geofenceId, incompleteOnly);
        return longValue(q);
    }

    public List<ChargingProcessDto> find(Long carId, Instant from, Instant to, Long geofenceId,
                                         String chargeType, Boolean incompleteOnly, int limit, int offset) {
        if (chargeType != null && !chargeType.isBlank()) {
            SqlQueryBuilder q = SqlQueryBuilder.of("""
                    SELECT * FROM (
                      SELECT
                        cp.id, cp.car_id, cp.start_date, cp.end_date,
                        cp.charge_energy_added, cp.charge_energy_used, cp.duration_min,
                        cp.start_battery_level, cp.end_battery_level,
                        cp.start_ideal_range_km, cp.end_ideal_range_km,
                        cp.start_rated_range_km, cp.end_rated_range_km,
                        cp.outside_temp_avg AS outside_temp_avg_c, cp.cost,
                        cp.position_id, cp.address_id, cp.geofence_id,
                        CASE WHEN NULLIF(mode() WITHIN GROUP (ORDER BY c.charger_phases), 0) IS NULL
                             THEN 'DC' ELSE 'AC' END AS charge_type
                      FROM charging_processes cp
                      LEFT JOIN charges c ON c.charging_process_id = cp.id
                      WHERE (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                    """);
            applyFilters(q, carId, from, to, geofenceId, incompleteOnly);
            q.append("""
                      GROUP BY cp.id
                    ) t WHERE t.charge_type = :chargeType
                    ORDER BY t.start_date DESC LIMIT :limit OFFSET :offset
                    """)
                    .bind("chargeType", chargeType.toUpperCase())
                    .bind("limit", limit)
                    .bind("offset", offset);
            return list(q, ChargingProcessDto.class);
        }

        SqlQueryBuilder q = SqlQueryBuilder.of(PROCESS_SELECT + """
                WHERE (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                """);
        applyFilters(q, carId, from, to, geofenceId, incompleteOnly);
        q.append(" ORDER BY cp.start_date DESC LIMIT :limit OFFSET :offset")
                .bind("limit", limit)
                .bind("offset", offset);
        return list(q, ChargingProcessDto.class);
    }

    public Optional<ChargingProcessDto> findById(long id) {
        return one(SqlQueryBuilder.of(PROCESS_SELECT + " WHERE cp.id = :id ").bind("id", id),
                ChargingProcessDto.class);
    }

    public List<ChargeSampleDto> findSamples(long processId) {
        return queryList("""
                SELECT id, date, battery_level, usable_battery_level, charge_energy_added,
                       charger_power, charger_voltage, charger_actual_current, charger_phases,
                       fast_charger_present, fast_charger_type,
                       ideal_battery_range_km, rated_battery_range_km, outside_temp, battery_heater_on
                FROM charges
                WHERE charging_process_id = :id
                ORDER BY date
                """, ChargeSampleDto.class, "id", processId);
    }

    public List<ChargingProcessEnrichedDto> findEnriched(Long carId, Instant from, Instant to, Long geofenceId,
                                                         Boolean incompleteOnly, String rangeMode,
                                                         int limit, int offset) {
        SqlQueryBuilder q = SqlQueryBuilder.of(enrichedSelect(rangeMode) + """
                  WHERE (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                """);
        applyFilters(q, carId, from, to, geofenceId, incompleteOnly);
        q.append("""
                  GROUP BY cp.id, cp.car_id, cp.start_date, cp.end_date, cp.charge_energy_added,
                           cp.charge_energy_used, cp.duration_min, cp.start_battery_level, cp.end_battery_level,
                           cp.outside_temp_avg, cp.cost, cp.geofence_id, cp.address_id, cp.position_id,
                           range_added_km, address, g.name, p.latitude, p.longitude, p.odometer, cars.efficiency
                ) t
                ORDER BY t.start_date DESC LIMIT :limit OFFSET :offset
                """)
                .bind("limit", limit)
                .bind("offset", offset);
        return list(q, ChargingProcessEnrichedDto.class);
    }

    public Optional<ChargingProcessEnrichedDto> findEnrichedById(long id, String rangeMode) {
        return one(SqlQueryBuilder.of(enrichedSelect(rangeMode) + """
                WHERE cp.id = :id
                GROUP BY cp.id, cp.car_id, cp.start_date, cp.end_date, cp.charge_energy_added,
                         cp.charge_energy_used, cp.duration_min, cp.start_battery_level, cp.end_battery_level,
                         cp.outside_temp_avg, cp.cost, cp.geofence_id, cp.address_id, cp.position_id,
                         range_added_km, address, g.name, p.latitude, p.longitude, p.odometer, cars.efficiency
                ) t
                """).bind("id", id), ChargingProcessEnrichedDto.class);
    }

    private static String enrichedSelect(String rangeMode) {
        String rangeAdded = "cp.end_" + rangeMode + "_range_km - cp.start_" + rangeMode + "_range_km";
        return """
                SELECT * FROM (
                  SELECT
                    cp.id, cp.car_id, cp.start_date, cp.end_date,
                    CONCAT_WS(', ',
                      COALESCE(a.name, NULLIF(CONCAT_WS(' ', a.road, a.house_number), '')), a.city) AS address,
                    g.name AS geofence_name,
                    cp.geofence_id, cp.address_id, cp.position_id,
                    p.latitude, p.longitude,
                    cp.charge_energy_added, cp.charge_energy_used, cp.duration_min,
                    cp.start_battery_level, cp.end_battery_level,
                    %s AS range_added_km,
                    cp.outside_temp_avg AS outside_temp_avg_c, cp.cost,
                    CASE WHEN NULLIF(mode() WITHIN GROUP (ORDER BY c.charger_phases), 0) IS NULL
                         THEN 'DC' ELSE 'AC' END AS charge_type,
                    max(c.charger_voltage) AS max_charger_voltage,
                    p.odometer,
                    cars.efficiency
                  FROM charging_processes cp
                  LEFT JOIN charges c ON c.charging_process_id = cp.id
                  LEFT JOIN positions p ON p.id = cp.position_id
                  LEFT JOIN cars ON cars.id = cp.car_id
                  LEFT JOIN addresses a ON a.id = cp.address_id
                  LEFT JOIN geofences g ON g.id = cp.geofence_id
                """.formatted(rangeAdded);
    }

    private static void applyFilters(SqlQueryBuilder q, Long carId, Instant from, Instant to,
                                     Long geofenceId, Boolean incompleteOnly) {
        q.andIfPresent(" AND cp.car_id = :carId", "carId", carId)
                .andIfPresent(" AND cp.start_date >= :from", "from", from)
                .andIfPresent(" AND cp.start_date <= :to", "to", to)
                .andIfPresent(" AND cp.geofence_id = :geofenceId", "geofenceId", geofenceId)
                .andIfTrue(" AND cp.end_date IS NULL", Boolean.TRUE.equals(incompleteOnly));
    }
}
