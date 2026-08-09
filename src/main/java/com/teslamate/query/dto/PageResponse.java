package com.teslamate.query.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long total
) {
    public static <T> PageResponse<T> of(List<T> data, int page, int size, long total) {
        return new PageResponse<>(data, page, size, total);
    }
}
