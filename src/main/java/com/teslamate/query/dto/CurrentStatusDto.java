package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CurrentStatusDto(
        Long carId,
        String model,
        String wheelType,
        ActivityStatus status,
        Long statusDurationMin,
        Integer batteryLevel,
        BigDecimal rangeKm,
        Double odometerKm,
        BigDecimal longitude,
        BigDecimal latitude,
        BigDecimal insideTempC,
        BigDecimal outsideTempC,
        Boolean climateOn,
        BigDecimal climateSetTempC,
        TirePressureDto tirePressure,
        Instant tirePressureTime,
        String firmwareVersion
) {
}
