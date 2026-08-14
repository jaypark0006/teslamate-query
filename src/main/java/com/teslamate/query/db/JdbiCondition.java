package com.teslamate.query.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic WHERE builder for single-table queries.
 * <p>
 * Usage: concatenate into SQL — {@code "SELECT * FROM drives " + condition.whereClause()}.
 * Do <b>not</b> feed fragments into StringTemplate: {@code <}/{@code >} in predicates
 * (e.g. {@code distance > 0}) would need escaping there.
 */
public abstract class JdbiCondition {

    protected final List<String> conditions = new ArrayList<>();
    protected final Map<String, Object> params = new LinkedHashMap<>();
    private final Map<String, String> sorts = new LinkedHashMap<>();

    /** Empty, or {@code WHERE a AND b} (includes the WHERE keyword). */
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

    protected final void in(String sqlColumn, String paramName, java.util.Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        conditions.add(sqlColumn + " IN (<" + paramName + ">)");
        params.put(paramName, values);
    }

    protected final void orderBy(String sqlColumn, String direction) {
        sorts.put(sqlColumn, direction);
    }
}
