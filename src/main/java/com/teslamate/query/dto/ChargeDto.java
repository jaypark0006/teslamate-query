package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Full row from {@code charges} table. */
public record ChargeDto(
        Long id,
        Long chargingProcessId,
        Instant date,
        Integer batteryLevel,
        Integer usableBatteryLevel,
        BigDecimal chargeEnergyAdded,
        Integer chargerPower,
        Integer chargerVoltage,
        Integer chargerActualCurrent,
        Integer chargerPhases,
        Boolean fastChargerPresent,
        String fastChargerType,
        BigDecimal idealBatteryRangeKm,
        BigDecimal ratedBatteryRangeKm,
        BigDecimal outsideTemp,
        Boolean batteryHeaterOn,
        Integer acVoltage,
        Integer acCurrent
) {
}
