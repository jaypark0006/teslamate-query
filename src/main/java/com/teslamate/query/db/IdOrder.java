package com.teslamate.query.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class IdOrder {
    private IdOrder() {}

    public static <T> List<T> align(Collection<Long> ids, List<T> rows, Function<T, Long> idFn) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Long, T> byId = rows.stream().collect(Collectors.toMap(idFn, Function.identity(), (a, b) -> a));
        List<T> out = new ArrayList<>(ids.size());
        for (Long id : ids) {
            T row = byId.get(id);
            if (row != null) {
                out.add(row);
            }
        }
        return out;
    }

    public static boolean isEmpty(Collection<?> ids) {
        return ids == null || ids.isEmpty();
    }
}
