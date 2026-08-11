package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.UpdateSearchCondition;
import com.teslamate.query.entity.UpdateEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** updates table — single-table Condition queries. */
@Repository
public class UpdateDao {

    private final Jdbi jdbi;

    public UpdateDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(UpdateSearchCondition condition) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("SELECT COUNT(*) FROM updates " + condition.whereClause());
            condition.params().forEach(q::bind);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(UpdateSearchCondition condition, int limit, int offset) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery(
                    "SELECT id FROM updates "
                            + condition.whereClause() + " "
                            + condition.sortClause()
                            + " LIMIT :limit OFFSET :offset");
            condition.params().forEach(q::bind);
            q.bind("limit", limit).bind("offset", offset);
            return q.mapTo(Long.class).list();
        });
    }

    public List<UpdateEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM updates WHERE id IN (<ids>)")
                .bindList("ids", ids)
                .map(ConstructorMapper.of(UpdateEntity.class))
                .list());
    }

    public List<UpdateEntity> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), UpdateEntity::id);
    }

    public Optional<UpdateEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM updates WHERE id = :id")
                .bind("id", id)
                .map(ConstructorMapper.of(UpdateEntity.class))
                .findOne());
    }
}
