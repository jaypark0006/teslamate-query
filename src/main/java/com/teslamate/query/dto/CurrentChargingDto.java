package com.teslamate.query.dto;

import java.math.BigDecimal;

public record CurrentChargingDto(
        Long carId,
        Long chargingProcessId,
        BigDecimal energyAddedKwh,
        Integer startBatteryLevel,
        BigDecimal startRangeKm,
        ChargeType chargeType,
        BigDecimal cost
) {
}
