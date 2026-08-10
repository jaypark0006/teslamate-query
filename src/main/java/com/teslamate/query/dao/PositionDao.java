package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PositionDto;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Positions only. Complex features compose via Service + multiple daos.
 */
@Repository
public class PositionDao {

    private static final String SELECT = """
            SELECT id, car_id, drive_id, date, latitude, longitude, elevation, speed, power,
                   odometer, ideal_battery_range_km, rated_battery_range_km,
                   battery_level, usable_battery_level, outside_temp, inside_temp
            FROM positions
            """;

    private final Jdbi jdbi;

    public PositionDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<PositionDto> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery(SELECT + " WHERE id IN (<ids>)")
                .bindList("ids", ids)
                .mapTo(PositionDto.class)
                .list());
    }

    public List<DrivePositionDto> findByDriveId(long driveId) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT id, date, latitude, longitude, elevation, speed, power,
                               odometer, ideal_battery_range_km, rated_battery_range_km,
                               battery_level, usable_battery_level, outside_temp, inside_temp
                        FROM positions
                        WHERE drive_id = :driveId
                        ORDER BY date
                        """)
                .bind("driveId", driveId)
                .mapTo(DrivePositionDto.class)
                .list());
    }

    /** All points for many drives, ordered by drive then time (for map grouping). */
    public List<PositionDto> findByDriveIds(Collection<Long> driveIds) {
        if (IdOrder.isEmpty(driveIds)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery(SELECT + """
                        WHERE drive_id IN (<driveIds>)
                        ORDER BY drive_id, date
                        """)
                .bindList("driveIds", driveIds)
                .mapTo(PositionDto.class)
                .list());
    }

    /** Battery / SOC series for Charge Level style panels (positions + optional clean). */
    public List<PositionDto> findForCarInRange(long carId, java.time.Instant from, java.time.Instant to,
                                               boolean cleanOnly, int limit) {
        String clean = cleanOnly ? " AND ideal_battery_range_km IS NOT NULL" : "";
        return jdbi.withHandle(h -> {
            Query q = h.createQuery(SELECT + """
                    WHERE car_id = :carId AND date >= :from AND date <= :to
                    """ + clean + """
                    ORDER BY date
                    LIMIT :limit
                    """);
            return q.bind("carId", carId)
                    .bind("from", from)
                    .bind("to", to)
                    .bind("limit", limit)
                    .mapTo(PositionDto.class)
                    .list();
        });
    }
}
