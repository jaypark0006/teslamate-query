package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.SqlQueryBuilder;
import com.teslamate.query.dto.ChargingProcessDto;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class ChargingProcessDao {

    private static final String SELECT = """
            SELECT
              cp.id, cp.car_id, cp.start_date, cp.end_date,
              cp.charge_energy_added, cp.charge_energy_used, cp.duration_min,
              cp.start_battery_level, cp.end_battery_level,
              cp.start_ideal_range_km, cp.end_ideal_range_km,
              cp.start_rated_range_km, cp.end_rated_range_km,
              cp.outside_temp_avg AS outside_temp_avg_c, cp.cost,
              cp.position_id, cp.address_id, cp.geofence_id
            FROM charging_processes cp
            """;

    private final Jdbi jdbi;

    public ChargingProcessDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(Long carId, Instant from, Instant to, Long geofenceId, Boolean incompleteOnly) {
        SqlQueryBuilder q = SqlQueryBuilder.of("""
                SELECT COUNT(*) FROM charging_processes cp
                WHERE (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                """);
        applyFilters(q, carId, from, to, geofenceId, incompleteOnly);
        return jdbi.withHandle(h -> q.createQuery(h).mapTo(Long.class).one());
    }

    public List<Long> findIds(Long carId, Instant from, Instant to, Long geofenceId, Boolean incompleteOnly,
                              int limit, int offset) {
        SqlQueryBuilder q = SqlQueryBuilder.of("""
                SELECT cp.id FROM charging_processes cp
                WHERE (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                """);
        applyFilters(q, carId, from, to, geofenceId, incompleteOnly);
        q.append(" ORDER BY cp.start_date DESC LIMIT :limit OFFSET :offset")
                .bind("limit", limit)
                .bind("offset", offset);
        return jdbi.withHandle(h -> q.createQuery(h).mapTo(Long.class).list());
    }

    public List<ChargingProcessDto> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery(SELECT + " WHERE cp.id IN (<ids>)")
                .bindList("ids", ids)
                .mapTo(ChargingProcessDto.class)
                .list());
    }

    public List<ChargingProcessDto> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), ChargingProcessDto::id);
    }

    public Optional<ChargingProcessDto> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery(SELECT + " WHERE cp.id = :id")
                .bind("id", id)
                .mapTo(ChargingProcessDto.class)
                .findOne());
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
