package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ChargingProcessDto(
        Long id,
        Long carId,
        Instant startDate,
        Instant endDate,
        String address,
        String geofenceName,
        Long geofenceId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal chargeEnergyAdded,
        BigDecimal chargeEnergyUsed,
        Integer durationMin,
        Integer startBatteryLevel,
        Integer endBatteryLevel,
        BigDecimal rangeAddedKm,
        BigDecimal outsideTempAvgC,
        BigDecimal cost,
        String chargeType,
        Integer maxChargerVoltage,
        Double odometer,
        Double efficiency
) {
}
