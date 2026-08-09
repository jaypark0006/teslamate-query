package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lean charging session: {@code charging_processes} columns + FK ids.
 * AC/DC, max voltage, address labels live in samples or enriched view.
 */
public record ChargingProcessDto(
        Long id,
        Long carId,
        Instant startDate,
        Instant endDate,
        BigDecimal chargeEnergyAdded,
        BigDecimal chargeEnergyUsed,
        Integer durationMin,
        Integer startBatteryLevel,
        Integer endBatteryLevel,
        BigDecimal startIdealRangeKm,
        BigDecimal endIdealRangeKm,
        BigDecimal startRatedRangeKm,
        BigDecimal endRatedRangeKm,
        BigDecimal outsideTempAvgC,
        BigDecimal cost,
        Long positionId,
        Long addressId,
        Long geofenceId
) {
}
