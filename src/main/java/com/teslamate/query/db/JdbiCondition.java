package com.teslamate.query.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ASRS-style dynamic WHERE builder for JDBI {@code @Define("whereClause")} + {@code @BindMap}.
 *
 * <pre>
 *   var c = DriveSearchCondition.builder().carId(1).startDateFrom(from).build();
 *   dao.findIds(c.whereClause(), c.params(), limit, offset);
 * </pre>
 *
 * {@link #whereClause()} is either empty or {@code WHERE a AND b} (including the WHERE keyword).
 */
public abstract class JdbiCondition {

    protected final List<String> conditions = new ArrayList<>();
    protected final Map<String, Object> params = new LinkedHashMap<>();
    private final Map<String, String> sorts = new LinkedHashMap<>();

    public final String whereClause() {
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
    }

    public final Map<String, Object> params() {
        return Collections.unmodifiableMap(params);
    }

    public final String sortClause() {
        if (sorts.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        sorts.forEach((col, dir) -> parts.add(col + " " + dir));
        return "ORDER BY " + String.join(", ", parts);
    }

    public final boolean isEmpty() {
        return conditions.isEmpty();
    }

    protected final void eq(String sqlColumn, String paramName, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String s && s.isBlank()) {
            return;
        }
        conditions.add(sqlColumn + " = :" + paramName);
        params.put(paramName, value);
    }

    protected final void gte(String sqlColumn, String paramName, Object value) {
        if (value == null) {
            return;
        }
        conditions.add(sqlColumn + " >= :" + paramName);
        params.put(paramName, value);
    }

    protected final void lte(String sqlColumn, String paramName, Object value) {
        if (value == null) {
            return;
        }
        conditions.add(sqlColumn + " <= :" + paramName);
        params.put(paramName, value);
    }

    protected final void raw(String fragment, String paramName, Object value) {
        if (value == null) {
            return;
        }
        conditions.add(fragment);
        params.put(paramName, value);
    }

    protected final void rawNoParam(String fragment, boolean when) {
        if (when) {
            conditions.add(fragment);
        }
    }

    protected final void orderBy(String sqlColumn, String direction) {
        sorts.put(sqlColumn, direction);
    }
}
