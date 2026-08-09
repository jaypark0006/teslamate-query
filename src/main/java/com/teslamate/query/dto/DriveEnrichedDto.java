package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Optional wide row for Grafana tables ({@code view=enriched}).
 * Not the default domain resource.
 */
public record DriveEnrichedDto(
        Long id,
        Long carId,
        Instant startDate,
        Instant endDate,
        String startAddress,
        String endAddress,
        Integer durationMin,
        Double distanceKm,
        Integer startBatteryLevel,
        Integer endBatteryLevel,
        BigDecimal startRangeKm,
        BigDecimal endRangeKm,
        BigDecimal rangeDiffKm,
        Double consumptionKwh,
        Double consumptionKwhPerKm,
        Double outsideTempAvgC,
        Double insideTempAvgC,
        Double avgSpeedKmh,
        Integer speedMax,
        Integer powerMax,
        Integer powerMin,
        Integer ascent,
        Integer descent,
        Double carEfficiency,
        Long startGeofenceId,
        Long endGeofenceId,
        Long startAddressId,
        Long endAddressId,
        Long startPositionId,
        Long endPositionId
) {
}
