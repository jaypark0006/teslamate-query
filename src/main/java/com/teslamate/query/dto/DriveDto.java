package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DriveDto(
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
        Long endGeofenceId
) {
}
