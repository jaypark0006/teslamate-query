package com.teslamate.query.dao;

import com.teslamate.query.db.ConditionBinder;
import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.ChargeSearchCondition;
import com.teslamate.query.entity.ChargeEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** charges table — single-table Condition queries. */
@Repository
public class ChargeDao {

    private final Jdbi jdbi;

    public ChargeDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(ChargeSearchCondition condition) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("SELECT COUNT(*) FROM charges " + condition.whereClause());
            ConditionBinder.bind(q, condition);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(ChargeSearchCondition condition, int limit, int offset) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery(
                    "SELECT id FROM charges "
                            + condition.whereClause() + " "
                            + condition.sortClause()
                            + " LIMIT :limit OFFSET :offset");
            ConditionBinder.bind(q, condition);
            q.bind("limit", limit).bind("offset", offset);
            return q.mapTo(Long.class).list();
        });
    }

    public List<ChargeEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM charges WHERE id IN (<ids>)")
                .bindList("ids", ids)
                .map(ConstructorMapper.of(ChargeEntity.class))
                .list());
    }

    public List<ChargeEntity> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), ChargeEntity::id);
    }

    public Optional<ChargeEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM charges WHERE id = :id")
                .bind("id", id)
                .map(ConstructorMapper.of(ChargeEntity.class))
                .findOne());
    }

    public List<ChargeEntity> findByProcessId(long processId) {
        ChargeSearchCondition c = ChargeSearchCondition.builder().chargingProcessId(processId).build();
        List<Long> ids = findIds(c, 50_000, 0);
        return findByIdsOrdered(ids);
    }

    public Optional<ChargeEntity> findLatestByProcessIds(Collection<Long> processIds) {
        if (IdOrder.isEmpty(processIds)) {
            return Optional.empty();
        }
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT * FROM charges WHERE charging_process_id IN (<processIds>) "
                                + "ORDER BY date DESC LIMIT 1")
                .bindList("processIds", processIds)
                .map(ConstructorMapper.of(ChargeEntity.class))
                .findOne());
    }
}
