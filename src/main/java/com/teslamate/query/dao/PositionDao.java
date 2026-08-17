package com.teslamate.query.dao;

import com.teslamate.query.db.ConditionBinder;
import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.PositionSearchCondition;
import com.teslamate.query.entity.PositionCommutePoint;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.PositionPathPoint;
import com.teslamate.query.entity.PositionTirePressure;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class PositionDao {

    private static final int DEFAULT_ID_CAP = 50_000;

    private final Jdbi jdbi;

    public PositionDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(PositionSearchCondition condition) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("""
                    SELECT COUNT(*) FROM positions
                    %s
                    """.formatted(condition.whereClause()));
            ConditionBinder.bind(q, condition);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(PositionSearchCondition condition, int limit, int offset) {
        int cap = Math.min(Math.max(limit, 1), DEFAULT_ID_CAP);
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("""
                    SELECT id FROM positions
                    %s
                    %s
                    LIMIT :limit OFFSET :offset
                    """.formatted(condition.whereClause(), condition.sortClause()));
            ConditionBinder.bind(q, condition);
            q.bind("limit", cap).bind("offset", offset);
            return q.mapTo(Long.class).list();
        });
    }

    /**
     * One id per time bucket (seconds) so list/series span the whole window.
     */
    public List<Long> findIdsBucketed(PositionSearchCondition condition, int bucketSeconds, int limit) {
        int bucket = Math.max(bucketSeconds, 1);
        int cap = Math.min(Math.max(limit, 1), DEFAULT_ID_CAP);
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("""
                    SELECT id FROM (
                      SELECT id, date,
                             ROW_NUMBER() OVER (
                               PARTITION BY FLOOR(EXTRACT(EPOCH FROM date) / :bucketSec)
                               ORDER BY date
                             ) AS rn
                      FROM positions
                      %s
                    ) t
                    WHERE rn = 1
                    ORDER BY date
                    LIMIT :limit
                    """.formatted(condition.whereClause()));
            ConditionBinder.bind(q, condition);
            q.bind("bucketSec", bucket).bind("limit", cap);
            return q.mapTo(Long.class).list();
        });
    }

    public List<PositionEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM positions
                WHERE id IN (<ids>)
                """)
                .bindList("ids", ids)
                .map(ConstructorMapper.of(PositionEntity.class))
                .list());
    }

    public List<PositionEntity> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), PositionEntity::id);
    }

    public Optional<PositionEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM positions
                WHERE id = :id
                """)
                .bind("id", id)
                .map(ConstructorMapper.of(PositionEntity.class))
                .findOne());
    }

    public Optional<PositionEntity> findLatestByCarId(long carId) {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM positions
                WHERE id = (
                    SELECT MAX(id) FROM positions WHERE car_id = :carId
                )
                """)
                .bind("carId", carId)
                .map(ConstructorMapper.of(PositionEntity.class))
                .findOne());
    }

    public Optional<PositionEntity> findLatestWithTpmsByCarId(long carId) {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM positions
                WHERE id = (
                    SELECT MAX(id) FROM positions
                    WHERE car_id = :carId
                      AND tpms_pressure_fl IS NOT NULL
                )
                """)
                .bind("carId", carId)
                .map(ConstructorMapper.of(PositionEntity.class))
                .findOne());
    }

    public Optional<PositionEntity> findLatestByDriveId(long driveId) {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM positions
                WHERE id = (
                    SELECT MAX(id) FROM positions WHERE drive_id = :driveId
                )
                """)
                .bind("driveId", driveId)
                .map(ConstructorMapper.of(PositionEntity.class))
                .findOne());
    }

    /**
     * TPMS samples for one drive. Null TPMS rows are removed before bucketing,
     * so sparse pressure updates are not discarded by the generic position sampler.
     */
    public List<PositionTirePressure> findTirePressuresByDriveId(
            long driveId,
            int bucketSeconds,
            int limit
    ) {
        int bucket = Math.max(bucketSeconds, 0);
        int cap = Math.min(Math.max(limit, 1), DEFAULT_ID_CAP);
        if (bucket <= 1) {
            return jdbi.withHandle(h -> h.createQuery("""
                    SELECT date, tpms_pressure_fl, tpms_pressure_fr,
                           tpms_pressure_rl, tpms_pressure_rr
                    FROM positions
                    WHERE drive_id = :driveId
                      AND (tpms_pressure_fl IS NOT NULL OR tpms_pressure_fr IS NOT NULL
                           OR tpms_pressure_rl IS NOT NULL OR tpms_pressure_rr IS NOT NULL)
                    ORDER BY date
                    LIMIT :limit
                    """)
                    .bind("driveId", driveId)
                    .bind("limit", cap)
                    .map(ConstructorMapper.of(PositionTirePressure.class))
                    .list());
        }
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT date, tpms_pressure_fl, tpms_pressure_fr,
                       tpms_pressure_rl, tpms_pressure_rr
                FROM (
                  SELECT date, tpms_pressure_fl, tpms_pressure_fr,
                         tpms_pressure_rl, tpms_pressure_rr,
                         ROW_NUMBER() OVER (
                           PARTITION BY FLOOR(EXTRACT(EPOCH FROM date) / :bucketSec)
                           ORDER BY date
                         ) AS rn
                  FROM positions
                  WHERE drive_id = :driveId
                    AND (tpms_pressure_fl IS NOT NULL OR tpms_pressure_fr IS NOT NULL
                         OR tpms_pressure_rl IS NOT NULL OR tpms_pressure_rr IS NOT NULL)
                ) samples
                WHERE rn = 1
                ORDER BY date
                LIMIT :limit
                """)
                .bind("driveId", driveId)
                .bind("bucketSec", bucket)
                .bind("limit", cap)
                .map(ConstructorMapper.of(PositionTirePressure.class))
                .list());
    }

    public List<PositionPathPoint> findPathPointsByDriveIds(Collection<Long> driveIds) {
        return findPathPointsByDriveIds(driveIds, 0);
    }

    /**
     * Path vertices for many drives. {@code bucketSeconds <= 1} returns every
     * GPS sample — shape thinning (Douglas–Peucker) happens in Java so corners
     * stay. Otherwise one sample per time bucket, plus each drive's first and last.
     * Never apply a global LIMIT: that chops later drive_ids mid-trip.
     */
    public List<PositionPathPoint> findPathPointsByDriveIds(Collection<Long> driveIds, int bucketSeconds) {
        if (IdOrder.isEmpty(driveIds)) {
            return List.of();
        }
        int bucket = Math.max(bucketSeconds, 0);
        if (bucket <= 1) {
            return jdbi.withHandle(h -> h.createQuery("""
                    SELECT drive_id, date, longitude, latitude
                    FROM positions
                    WHERE drive_id IN (<driveIds>)
                      AND longitude IS NOT NULL
                      AND latitude IS NOT NULL
                    ORDER BY drive_id, date
                    """)
                    .bindList("driveIds", driveIds)
                    .map(ConstructorMapper.of(PositionPathPoint.class))
                    .list());
        }
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT drive_id, date, longitude, latitude
                FROM (
                    SELECT drive_id, date, longitude, latitude,
                           ROW_NUMBER() OVER (
                               PARTITION BY drive_id, FLOOR(EXTRACT(EPOCH FROM date) / :bucketSec)
                               ORDER BY date
                           ) AS rn,
                           ROW_NUMBER() OVER (PARTITION BY drive_id ORDER BY date) AS first_rn,
                           ROW_NUMBER() OVER (PARTITION BY drive_id ORDER BY date DESC) AS last_rn
                    FROM positions
                    WHERE drive_id IN (<driveIds>)
                      AND longitude IS NOT NULL
                      AND latitude IS NOT NULL
                ) t
                WHERE rn = 1 OR first_rn = 1 OR last_rn = 1
                ORDER BY drive_id, date
                """)
                .bindList("driveIds", driveIds)
                .bind("bucketSec", bucket)
                .map(ConstructorMapper.of(PositionPathPoint.class))
                .list());
    }

    /**
     * Commute overlay: one sample per time bucket plus first/last, including speed.
     */
    public List<PositionCommutePoint> findCommutePointsByDriveIds(Collection<Long> driveIds, int bucketSeconds) {
        if (IdOrder.isEmpty(driveIds)) {
            return List.of();
        }
        int bucket = Math.max(bucketSeconds, 15);
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT drive_id, date, longitude, latitude, speed, odometer
                FROM (
                    SELECT drive_id, date, longitude, latitude, speed, odometer,
                           ROW_NUMBER() OVER (
                               PARTITION BY drive_id, FLOOR(EXTRACT(EPOCH FROM date) / :bucketSec)
                               ORDER BY date
                           ) AS rn,
                           ROW_NUMBER() OVER (PARTITION BY drive_id ORDER BY date) AS first_rn,
                           ROW_NUMBER() OVER (PARTITION BY drive_id ORDER BY date DESC) AS last_rn
                    FROM positions
                    WHERE drive_id IN (<driveIds>)
                      AND longitude IS NOT NULL
                      AND latitude IS NOT NULL
                ) t
                WHERE rn = 1 OR first_rn = 1 OR last_rn = 1
                ORDER BY drive_id, date
                """)
                .bindList("driveIds", driveIds)
                .bind("bucketSec", bucket)
                .map(ConstructorMapper.of(PositionCommutePoint.class))
                .list());
    }
}
