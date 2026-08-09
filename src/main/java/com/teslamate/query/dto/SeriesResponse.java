package com.teslamate.query.dto;

import java.util.List;

public record SeriesResponse(
        List<Series> series
) {
    public record Series(String name, List<List<Object>> points) {
    }

    public static SeriesResponse of(String name, List<List<Object>> points) {
        return new SeriesResponse(List.of(new Series(name, points)));
    }

    public static SeriesResponse of(List<Series> series) {
        return new SeriesResponse(series);
    }
}
