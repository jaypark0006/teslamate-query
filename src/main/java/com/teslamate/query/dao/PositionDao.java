package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.PositionSearchCondition;
import com.teslamate.query.entity.PositionEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** positions table — single-table Condition queries. */
@Repository
public class PositionDao {

    private final Jdbi jdbi;

    public PositionDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(PositionSearchCondition condition) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("SELECT COUNT(*) FROM positions " + condition.whereClause());
            condition.params().forEach(q::bind);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(PositionSearchCondition condition, int limit, int offset) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery(
                    "SELECT id FROM positions "
                            + condition.whereClause() + " "
                            + condition.sortClause()
                            + " LIMIT :limit OFFSET :offset");
            condition.params().forEach(q::bind);
            q.bind("limit", limit).bind("offset", offset);
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

    /** Convenience for multi-DAO composition (drive → positions). */
    public List<PositionEntity> findByDriveId(long driveId) {
        PositionSearchCondition c = PositionSearchCondition.builder().driveId(driveId).build();
        List<Long> ids = findIds(c, 500_000, 0);
        return findByIdsOrdered(ids);
    }

    public List<PositionEntity> findByDriveIds(Collection<Long> driveIds) {
        if (IdOrder.isEmpty(driveIds)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT * FROM positions WHERE drive_id IN (<driveIds>) ORDER BY drive_id, date")
                .bindList("driveIds", driveIds)
                .map(ConstructorMapper.of(PositionEntity.class))
                .list());
    }
}
