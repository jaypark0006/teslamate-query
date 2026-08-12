package com.teslamate.query.db;

import org.jdbi.v3.core.statement.Query;

import java.util.Collection;

public final class ConditionBinder {
    private ConditionBinder() {}

    public static void bind(Query query, JdbiCondition condition) {
        condition.params().forEach((name, value) -> {
            if (value instanceof Collection<?> col) {
                if (!col.isEmpty()) {
                    query.bindList(name, col);
                }
            } else {
                query.bind(name, value);
            }
        });
    }
}
