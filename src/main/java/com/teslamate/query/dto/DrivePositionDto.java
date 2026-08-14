package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DrivePositionDto(
        Long positionId,
        Instant date,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer elevation,
        Integer speed,
        Integer power,
        Double odometer,
        BigDecimal idealBatteryRangeKm,
        BigDecimal ratedBatteryRangeKm,
        Integer batteryLevel,
        Integer usableBatteryLevel,
        BigDecimal outsideTemp,
        BigDecimal insideTemp
) {
}
