package com.teslamate.query.dao;

import com.teslamate.query.db.ConditionBinder;
import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.GeofenceSearchCondition;
import com.teslamate.query.entity.GeofenceEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** geofences table — single-table Condition queries. */
@Repository
public class GeofenceDao {

    private final Jdbi jdbi;

    public GeofenceDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(GeofenceSearchCondition condition) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("SELECT COUNT(*) FROM geofences " + condition.whereClause());
            ConditionBinder.bind(q, condition);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(GeofenceSearchCondition condition, int limit, int offset) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery(
                    "SELECT id FROM geofences "
                            + condition.whereClause() + " "
                            + condition.sortClause()
                            + " LIMIT :limit OFFSET :offset");
            ConditionBinder.bind(q, condition);
            q.bind("limit", limit).bind("offset", offset);
            return q.mapTo(Long.class).list();
        });
    }

    public List<GeofenceEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM geofences WHERE id IN (<ids>)")
                .bindList("ids", ids)
                .map(ConstructorMapper.of(GeofenceEntity.class))
                .list());
    }

    public List<GeofenceEntity> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), GeofenceEntity::id);
    }

    public Optional<GeofenceEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM geofences WHERE id = :id")
                .bind("id", id)
                .map(ConstructorMapper.of(GeofenceEntity.class))
                .findOne());
    }

}
