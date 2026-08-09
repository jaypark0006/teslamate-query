package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Optional wide row for Grafana charge tables ({@code view=enriched}).
 */
public record ChargingProcessEnrichedDto(
        Long id,
        Long carId,
        Instant startDate,
        Instant endDate,
        String address,
        String geofenceName,
        Long geofenceId,
        Long addressId,
        Long positionId,
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
