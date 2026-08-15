package com.teslamate.query.dao;

import com.teslamate.query.db.ConditionBinder;
import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.PositionSearchCondition;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.PositionPathPoint;
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
            Query q = h.createQuery("SELECT COUNT(*) FROM positions " + condition.whereClause());
            ConditionBinder.bind(q, condition);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(PositionSearchCondition condition, int limit, int offset) {
        int cap = Math.min(Math.max(limit, 1), DEFAULT_ID_CAP);
        return jdbi.withHandle(h -> {
            Query q = h.createQuery(
                    "SELECT id FROM positions "
                            + condition.whereClause() + " "
                            + condition.sortClause()
                            + " LIMIT :limit OFFSET :offset");
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
            Query q = h.createQuery(
                    "SELECT id FROM ("
                            + " SELECT id, date, ROW_NUMBER() OVER ("
                            + "   PARTITION BY FLOOR(EXTRACT(EPOCH FROM date) / :bucketSec) ORDER BY date"
                            + " ) AS rn FROM positions "
                            + condition.whereClause()
                            + ") t WHERE rn = 1 ORDER BY date LIMIT :limit");
            ConditionBinder.bind(q, condition);
            q.bind("bucketSec", bucket).bind("limit", cap);
            return q.mapTo(Long.class).list();
        });
    }

    public List<PositionEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM positions WHERE id IN (<ids>)")
                .bindList("ids", ids)
                .map(ConstructorMapper.of(PositionEntity.class))
                .list());
    }

    public List<PositionEntity> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), PositionEntity::id);
    }

    public Optional<PositionEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM positions WHERE id = :id")
                .bind("id", id)
                .map(ConstructorMapper.of(PositionEntity.class))
                .findOne());
    }

    public Optional<PositionEntity> findLatestByCarId(long carId) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT * FROM positions WHERE id = ("
                                + "SELECT MAX(id) FROM positions WHERE car_id = :carId)")
                .bind("carId", carId)
                .map(ConstructorMapper.of(PositionEntity.class))
                .findOne());
    }

    public Optional<PositionEntity> findLatestWithTpmsByCarId(long carId) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT * FROM positions WHERE id = ("
                                + "SELECT MAX(id) FROM positions WHERE car_id = :carId "
                                + "AND tpms_pressure_fl IS NOT NULL)")
                .bind("carId", carId)
                .map(ConstructorMapper.of(PositionEntity.class))
                .findOne());
    }

    public Optional<PositionEntity> findLatestByDriveId(long driveId) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT * FROM positions WHERE id = ("
                                + "SELECT MAX(id) FROM positions WHERE drive_id = :driveId)")
                .bind("driveId", driveId)
                .map(ConstructorMapper.of(PositionEntity.class))
                .findOne());
    }

    public List<PositionPathPoint> findPathPointsByDriveIds(Collection<Long> driveIds) {
        if (IdOrder.isEmpty(driveIds)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT drive_id, date, longitude, latitude FROM positions "
                                + "WHERE drive_id IN (<driveIds>) AND longitude IS NOT NULL AND latitude IS NOT NULL "
                                + "ORDER BY drive_id, date")
                .bindList("driveIds", driveIds)
                .map(ConstructorMapper.of(PositionPathPoint.class))
                .list());
    }
}
