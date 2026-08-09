package com.teslamate.query.db;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Small helper for dynamic SQL + named binds on top of JDBI.
 * Keeps repositories free of ResultSet conversion noise.
 */
public final class SqlQueryBuilder {

    private final StringBuilder sql;
    private final Map<String, Object> binds = new LinkedHashMap<>();

    private SqlQueryBuilder(String baseSql) {
        this.sql = new StringBuilder(baseSql);
    }

    public static SqlQueryBuilder of(String baseSql) {
        return new SqlQueryBuilder(baseSql);
    }

    public SqlQueryBuilder append(String fragment) {
        sql.append(fragment);
        return this;
    }

    public SqlQueryBuilder bind(String name, Object value) {
        binds.put(name, value);
        return this;
    }

    /** Append {@code AND ...} and bind only when value is non-null (and non-blank for strings). */
    public SqlQueryBuilder andIfPresent(String fragment, String bindName, Object value) {
        if (value == null) {
            return this;
        }
        if (value instanceof String s && s.isBlank()) {
            return this;
        }
        sql.append(fragment);
        binds.put(bindName, value);
        return this;
    }

    public SqlQueryBuilder andIfTrue(String fragment, boolean condition) {
        if (condition) {
            sql.append(fragment);
        }
        return this;
    }

    public String sql() {
        return sql.toString();
    }

    public Map<String, Object> binds() {
        return binds;
    }

    public Query createQuery(Handle handle) {
        Query query = handle.createQuery(sql.toString());
        binds.forEach(query::bind);
        return query;
    }

    public <T> T withQuery(Handle handle, java.util.function.Function<Query, T> fn) {
        return fn.apply(createQuery(handle));
    }

    public void applyBinds(Query query) {
        binds.forEach(query::bind);
    }

    public static Object instant(Instant instant) {
        return instant; // JavaTime / postgres plugin handles Instant
    }
}
