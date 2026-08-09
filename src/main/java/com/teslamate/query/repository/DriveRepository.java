package com.teslamate.query.repository;

import com.teslamate.query.db.JdbiRepository;
import com.teslamate.query.db.SqlQueryBuilder;
import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class DriveRepository extends JdbiRepository {

    private static final String DRIVE_SELECT = """
            SELECT
              d.id,
              d.car_id,
              d.start_date,
              d.end_date,
              d.duration_min,
              d.distance AS distance_km,
              d.start_ideal_range_km,
              d.end_ideal_range_km,
              d.start_rated_range_km,
              d.end_rated_range_km,
              d.outside_temp_avg AS outside_temp_avg_c,
              d.inside_temp_avg AS inside_temp_avg_c,
              CASE WHEN d.duration_min > 0
                   THEN d.distance / (d.duration_min * 60.0) * 3600.0
                   ELSE NULL END AS avg_speed_kmh,
              d.speed_max,
              d.power_max,
              d.power_min,
              d.ascent,
              d.descent,
              d.start_position_id,
              d.end_position_id,
              d.start_address_id,
              d.end_address_id,
              d.start_geofence_id,
              d.end_geofence_id
            FROM drives d
            """;

    public DriveRepository(Jdbi jdbi) {
        super(jdbi);
    }

    public long count(Long carId, Instant from, Instant to, Double minDistance, Integer minDuration,
                      Long geofenceId, Boolean incompleteOnly) {
        SqlQueryBuilder q = SqlQueryBuilder.of("SELECT COUNT(*) FROM drives d WHERE 1=1 ");
        applyFilters(q, carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly);
        return longValue(q);
    }

    public List<DriveDto> find(Long carId, Instant from, Instant to, Double minDistance, Integer minDuration,
                               Long geofenceId, Boolean incompleteOnly, int limit, int offset) {
        SqlQueryBuilder q = SqlQueryBuilder.of(DRIVE_SELECT + " WHERE 1=1 ");
        applyFilters(q, carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly);
        q.append(" ORDER BY d.start_date DESC LIMIT :limit OFFSET :offset")
                .bind("limit", limit)
                .bind("offset", offset);
        return list(q, DriveDto.class);
    }

    public Optional<DriveDto> findById(long id) {
        return one(SqlQueryBuilder.of(DRIVE_SELECT + " WHERE d.id = :id ").bind("id", id), DriveDto.class);
    }

    public List<DrivePositionDto> findPositions(long driveId, Integer downsampleSeconds) {
        if (downsampleSeconds != null && downsampleSeconds > 0) {
            return queryList("""
                    SELECT DISTINCT ON (bucket) id, date, latitude, longitude, elevation, speed, power,
                           odometer, ideal_battery_range_km, rated_battery_range_km,
                           battery_level, usable_battery_level, outside_temp, inside_temp
                    FROM (
                      SELECT *, floor(extract(epoch FROM date) / :bucket) AS bucket
                      FROM positions
                      WHERE drive_id = :driveId
                    ) t
                    ORDER BY bucket, date
                    """, DrivePositionDto.class,
                    "driveId", driveId, "bucket", downsampleSeconds);
        }
        return queryList("""
                SELECT id, date, latitude, longitude, elevation, speed, power,
                       odometer, ideal_battery_range_km, rated_battery_range_km,
                       battery_level, usable_battery_level, outside_temp, inside_temp
                FROM positions
                WHERE drive_id = :driveId
                ORDER BY date
                """, DrivePositionDto.class, "driveId", driveId);
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
