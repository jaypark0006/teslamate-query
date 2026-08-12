package com.teslamate.query.dao;

import com.teslamate.query.db.ConditionBinder;
import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.entity.ChargingProcessEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class ChargingProcessDao {

    private final Jdbi jdbi;

    public ChargingProcessDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(ChargingProcessSearchCondition condition) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("SELECT COUNT(*) FROM charging_processes " + condition.whereClause());
            ConditionBinder.bind(q, condition);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(ChargingProcessSearchCondition condition, int limit, int offset) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery(
                    "SELECT id FROM charging_processes "
                            + condition.whereClause() + " "
                            + condition.sortClause()
                            + " LIMIT :limit OFFSET :offset");
            ConditionBinder.bind(q, condition);
            q.bind("limit", limit).bind("offset", offset);
            return q.mapTo(Long.class).list();
        });
    }

    public List<ChargingProcessEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM charging_processes WHERE id IN (<ids>)")
                .bindList("ids", ids)
                .map(ConstructorMapper.of(ChargingProcessEntity.class))
                .list());
    }

    public List<ChargingProcessEntity> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), ChargingProcessEntity::id);
    }

    public Optional<ChargingProcessEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM charging_processes WHERE id = :id")
                .bind("id", id)
                .map(ConstructorMapper.of(ChargingProcessEntity.class))
                .findOne());
    }
}
