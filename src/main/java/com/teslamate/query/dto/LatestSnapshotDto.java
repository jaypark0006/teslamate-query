package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LatestSnapshotDto(
        Long carId,
        Instant date,
        String source,
        Integer batteryLevel,
        Integer usableBatteryLevel,
        BigDecimal idealBatteryRangeKm,
        BigDecimal ratedBatteryRangeKm,
        Double odometerKm,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal outsideTempC,
        BigDecimal insideTempC,
        Integer speed,
        Integer power,
        Integer chargerPower,
        Integer chargerVoltage
) {
}
