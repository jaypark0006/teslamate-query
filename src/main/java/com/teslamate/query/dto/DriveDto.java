package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lean drive resource: columns from {@code drives} only (+ simple derived metrics).
 * Related entities are referenced by id; compose names via /addresses, /geofences, /cars.
 */
public record DriveDto(
        Long driveId,
        Long carId,
        Instant startDate,
        Instant endDate,
        Integer durationMin,
        Double distanceKm,
        BigDecimal startIdealRangeKm,
        BigDecimal endIdealRangeKm,
        BigDecimal startRatedRangeKm,
        BigDecimal endRatedRangeKm,
        Double outsideTempAvgC,
        Double insideTempAvgC,
        Double avgSpeedKmh,
        Integer speedMax,
        Integer powerMax,
        Integer powerMin,
        Integer ascent,
        Integer descent,
        Long startPositionId,
        Long endPositionId,
        Long startAddressId,
        Long endAddressId,
        Long startGeofenceId,
        Long endGeofenceId
) {
}
