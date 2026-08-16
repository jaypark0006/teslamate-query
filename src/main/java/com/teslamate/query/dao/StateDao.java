package com.teslamate.query.dao;

import com.teslamate.query.db.ConditionBinder;
import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.StateSearchCondition;
import com.teslamate.query.entity.StateEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** states table — single-table Condition queries. */
@Repository
public class StateDao {

    private final Jdbi jdbi;

    public StateDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(StateSearchCondition condition) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("""
                    SELECT COUNT(*) FROM states
                    %s
                    """.formatted(condition.whereClause()));
            ConditionBinder.bind(q, condition);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(StateSearchCondition condition, int limit, int offset) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("""
                    SELECT id FROM states
                    %s
                    %s
                    LIMIT :limit OFFSET :offset
                    """.formatted(condition.whereClause(), condition.sortClause()));
            ConditionBinder.bind(q, condition);
            q.bind("limit", limit).bind("offset", offset);
            return q.mapTo(Long.class).list();
        });
    }

    public List<StateEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        // state is a PG enum — cast to text for stable String mapping
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT id, car_id, state::text AS state, start_date, end_date
                FROM states
                WHERE id IN (<ids>)
                """)
                .bindList("ids", ids)
                .map(ConstructorMapper.of(StateEntity.class))
                .list());
    }

    public List<StateEntity> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), StateEntity::id);
    }

    public Optional<StateEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT id, car_id, state::text AS state, start_date, end_date
                FROM states
                WHERE id = :id
                """)
                .bind("id", id)
                .map(ConstructorMapper.of(StateEntity.class))
                .findOne());
    }
}
