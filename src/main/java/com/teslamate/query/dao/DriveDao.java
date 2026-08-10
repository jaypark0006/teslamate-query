package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.SqlQueryBuilder;
import com.teslamate.query.dto.DriveDto;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class DriveDao {

    private static final String SELECT = """
            SELECT
              d.id, d.car_id, d.start_date, d.end_date, d.duration_min,
              d.distance AS distance_km,
              d.start_ideal_range_km, d.end_ideal_range_km,
              d.start_rated_range_km, d.end_rated_range_km,
              d.outside_temp_avg AS outside_temp_avg_c,
              d.inside_temp_avg AS inside_temp_avg_c,
              CASE WHEN d.duration_min > 0
                   THEN d.distance / (d.duration_min * 60.0) * 3600.0
                   ELSE NULL END AS avg_speed_kmh,
              d.speed_max, d.power_max, d.power_min, d.ascent, d.descent,
              d.start_position_id, d.end_position_id,
              d.start_address_id, d.end_address_id,
              d.start_geofence_id, d.end_geofence_id
            FROM drives d
            """;

    private final Jdbi jdbi;

    public DriveDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(Long carId, Instant from, Instant to, Double minDistance, Integer minDuration,
                      Long geofenceId, Boolean incompleteOnly) {
        SqlQueryBuilder q = SqlQueryBuilder.of("SELECT COUNT(*) FROM drives d WHERE 1=1 ");
        applyFilters(q, carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly);
        return jdbi.withHandle(h -> q.createQuery(h).mapTo(Long.class).one());
    }

    /** Step 1: filter → ids only */
    public List<Long> findIds(Long carId, Instant from, Instant to, Double minDistance, Integer minDuration,
                              Long geofenceId, Boolean incompleteOnly, int limit, int offset) {
        SqlQueryBuilder q = SqlQueryBuilder.of("SELECT d.id FROM drives d WHERE 1=1 ");
        applyFilters(q, carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly);
        q.append(" ORDER BY d.start_date DESC LIMIT :limit OFFSET :offset")
                .bind("limit", limit)
                .bind("offset", offset);
        return jdbi.withHandle(h -> q.createQuery(h).mapTo(Long.class).list());
    }

    /** Step 2: load rows by ids (order not guaranteed — use IdOrder.align) */
    public List<DriveDto> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery(SELECT + " WHERE d.id IN (<ids>)")
                .bindList("ids", ids)
                .mapTo(DriveDto.class)
                .list());
    }

    public List<DriveDto> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), DriveDto::id);
    }

    public Optional<DriveDto> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery(SELECT + " WHERE d.id = :id")
                .bind("id", id)
                .mapTo(DriveDto.class)
                .findOne());
    }

    private static void applyFilters(SqlQueryBuilder q, Long carId, Instant from, Instant to,
                                     Double minDistance, Integer minDuration, Long geofenceId,
                                     Boolean incompleteOnly) {
        q.andIfPresent(" AND d.car_id = :carId", "carId", carId)
                .andIfPresent(" AND d.start_date >= :from", "from", from)
                .andIfPresent(" AND d.start_date <= :to", "to", to)
                .andIfPresent(" AND d.distance >= :minDistance", "minDistance", minDistance)
                .andIfPresent(" AND d.duration_min >= :minDuration", "minDuration", minDuration)
                .andIfPresent(" AND (d.start_geofence_id = :geofenceId OR d.end_geofence_id = :geofenceId)",
                        "geofenceId", geofenceId)
                .andIfTrue(" AND d.end_date IS NULL", Boolean.TRUE.equals(incompleteOnly));
    }
}
