package com.teslamate.query.repository;

import com.teslamate.query.db.JdbiRepository;
import com.teslamate.query.db.SqlQueryBuilder;
import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
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
            // Only join charges when filtering by AC/DC
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

    private static void applyFilters(SqlQueryBuilder q, Long carId, Instant from, Instant to,
                                     Long geofenceId, Boolean incompleteOnly) {
        q.andIfPresent(" AND cp.car_id = :carId", "carId", carId)
                .andIfPresent(" AND cp.start_date >= :from", "from", from)
                .andIfPresent(" AND cp.start_date <= :to", "to", to)
                .andIfPresent(" AND cp.geofence_id = :geofenceId", "geofenceId", geofenceId)
                .andIfTrue(" AND cp.end_date IS NULL", Boolean.TRUE.equals(incompleteOnly));
    }
}
