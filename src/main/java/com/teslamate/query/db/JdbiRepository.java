package com.teslamate.query.db;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;

import java.util.List;
import java.util.Optional;

/**
 * Base for repositories: all DB access goes through JDBI with constructor-mapped types.
 */
public abstract class JdbiRepository {

    protected final Jdbi jdbi;

    protected JdbiRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    protected <T> T inHandle(HandleCallback<T, RuntimeException> callback) {
        return jdbi.withHandle(callback);
    }

    protected <T> List<T> list(SqlQueryBuilder builder, Class<T> type) {
        return inHandle(h -> builder.createQuery(h).mapTo(type).list());
    }

    protected <T> Optional<T> one(SqlQueryBuilder builder, Class<T> type) {
        return inHandle(h -> builder.createQuery(h).mapTo(type).findOne());
    }

    protected long longValue(SqlQueryBuilder builder) {
        return inHandle(h -> builder.createQuery(h).mapTo(Long.class).one());
    }

    protected <T> Optional<T> queryOne(String sql, Class<T> type, Object... kvPairs) {
        return inHandle(h -> {
            Query q = h.createQuery(sql);
            bindPairs(q, kvPairs);
            return q.mapTo(type).findOne();
        });
    }

    protected <T> List<T> queryList(String sql, Class<T> type, Object... kvPairs) {
        return inHandle(h -> {
            Query q = h.createQuery(sql);
            bindPairs(q, kvPairs);
            return q.mapTo(type).list();
        });
    }

    protected long queryLong(String sql, Object... kvPairs) {
        return inHandle(h -> {
            Query q = h.createQuery(sql);
            bindPairs(q, kvPairs);
            return q.mapTo(Long.class).one();
        });
    }

    protected Double queryDouble(String sql, Object... kvPairs) {
        return inHandle(h -> {
            Query q = h.createQuery(sql);
            bindPairs(q, kvPairs);
            return q.mapTo(Double.class).findOne().orElse(null);
        });
    }

    protected String queryString(String sql, Object... kvPairs) {
        return inHandle(h -> {
            Query q = h.createQuery(sql);
            bindPairs(q, kvPairs);
            return q.mapTo(String.class).findOne().orElse(null);
        });
    }

    protected Boolean queryBoolean(String sql, Object... kvPairs) {
        return inHandle(h -> {
            Query q = h.createQuery(sql);
            bindPairs(q, kvPairs);
            return q.mapTo(Boolean.class).findOne().orElse(null);
        });
    }

    private static void bindPairs(Query q, Object... kvPairs) {
        if (kvPairs.length % 2 != 0) {
            throw new IllegalArgumentException("bind pairs must be even: name, value, ...");
        }
        for (int i = 0; i < kvPairs.length; i += 2) {
            q.bind(String.valueOf(kvPairs[i]), kvPairs[i + 1]);
        }
    }
}
