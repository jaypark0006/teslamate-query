package com.teslamate.query.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long total,
        UnitsMeta units
) {
    public static <T> PageResponse<T> of(List<T> data, int page, int size, long total) {
        return of(data, page, size, total, UnitsMeta.METRIC);
    }

    public static <T> PageResponse<T> of(List<T> data, int page, int size, long total, UnitsMeta units) {
        return new PageResponse<>(data, page, size, total, units == null ? UnitsMeta.METRIC : units);
    }
}
