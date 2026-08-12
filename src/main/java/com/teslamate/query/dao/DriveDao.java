package com.teslamate.query.dao;

import com.teslamate.query.db.ConditionBinder;
import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.entity.DriveEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class DriveDao {

    private final Jdbi jdbi;

    public DriveDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(DriveSearchCondition condition) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("SELECT COUNT(*) FROM drives " + condition.whereClause());
            ConditionBinder.bind(q, condition);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(DriveSearchCondition condition, int limit, int offset) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery(
                    "SELECT id FROM drives "
                            + condition.whereClause() + " "
                            + condition.sortClause()
                            + " LIMIT :limit OFFSET :offset");
            ConditionBinder.bind(q, condition);
            q.bind("limit", limit).bind("offset", offset);
            return q.mapTo(Long.class).list();
        });
    }

    public List<DriveEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM drives WHERE id IN (<ids>)")
                .bindList("ids", ids)
                .map(ConstructorMapper.of(DriveEntity.class))
                .list());
    }

    public List<DriveEntity> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), DriveEntity::id);
    }

    public Optional<DriveEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM drives WHERE id = :id")
                .bind("id", id)
                .map(ConstructorMapper.of(DriveEntity.class))
                .findOne());
    }
}
