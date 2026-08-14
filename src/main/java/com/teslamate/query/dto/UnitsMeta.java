package com.teslamate.query.dto;

/**
 * Units used for numeric fields in this response body.
 * Length-bearing fields (distance, *Range*, odometer, …) use {@code length};
 * temperature fields use {@code temperature}; speed uses {@code speed}; elevation uses {@code elevation}.
 */
public record UnitsMeta(
        String length,
        String temperature,
        String speed,
        String elevation
) {
    public static final UnitsMeta METRIC = new UnitsMeta("km", "C", "km/h", "m");
}
