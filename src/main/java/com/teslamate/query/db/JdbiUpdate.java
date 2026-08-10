package com.teslamate.query.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ASRS-style dynamic SET builder for JDBI {@code @Define("setClause")} + {@code @BindMap}.
 * TeslaMate-query is mostly read-only; kept for parity and future admin writes.
 */
public abstract class JdbiUpdate {

    protected final List<String> assignments = new ArrayList<>();
    protected final Map<String, Object> params = new LinkedHashMap<>();

    public final String setClause() {
        return assignments.isEmpty() ? "" : "SET " + String.join(", ", assignments);
    }

    public final boolean isEmpty() {
        return assignments.isEmpty();
    }

    public final Map<String, Object> params() {
        return Collections.unmodifiableMap(params);
    }

    protected final void set(String sqlColumn, String paramName, Object value) {
        if (value == null) {
            return;
        }
        assignments.add(sqlColumn + " = :" + paramName);
        params.put(paramName, value);
    }
}
