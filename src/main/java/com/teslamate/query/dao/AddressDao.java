package com.teslamate.query.dao;

import com.teslamate.query.db.ConditionBinder;
import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.AddressSearchCondition;
import com.teslamate.query.entity.AddressEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** addresses table — single-table Condition queries. */
@Repository
public class AddressDao {

    private final Jdbi jdbi;

    public AddressDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public long count(AddressSearchCondition condition) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("""
                    SELECT COUNT(*) FROM addresses
                    %s
                    """.formatted(condition.whereClause()));
            ConditionBinder.bind(q, condition);
            return q.mapTo(Long.class).one();
        });
    }

    public List<Long> findIds(AddressSearchCondition condition, int limit, int offset) {
        return jdbi.withHandle(h -> {
            Query q = h.createQuery("""
                    SELECT id FROM addresses
                    %s
                    %s
                    LIMIT :limit OFFSET :offset
                    """.formatted(condition.whereClause(), condition.sortClause()));
            ConditionBinder.bind(q, condition);
            q.bind("limit", limit).bind("offset", offset);
            return q.mapTo(Long.class).list();
        });
    }

    public List<AddressEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM addresses
                WHERE id IN (<ids>)
                """)
                .bindList("ids", ids)
                .map(ConstructorMapper.of(AddressEntity.class))
                .list());
    }

    public List<AddressEntity> findByIdsOrdered(Collection<Long> ids) {
        return IdOrder.align(ids, findByIds(ids), AddressEntity::id);
    }

    public Optional<AddressEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM addresses
                WHERE id = :id
                """)
                .bind("id", id)
                .map(ConstructorMapper.of(AddressEntity.class))
                .findOne());
    }
}
