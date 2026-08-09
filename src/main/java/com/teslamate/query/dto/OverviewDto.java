package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OverviewDto(
        Long carId,
        Instant from,
        Instant to,
        LatestSnapshotDto latest,
        Double totalDistanceKm,
        Double netConsumptionWhPerKm,
        Double grossConsumptionWhPerKm,
        Double totalChargeEnergyAddedKwh,
        Double totalChargeCost,
        Long driveCount,
        Long chargeCount,
        String firmwareVersion,
        Boolean lfpBattery
) {
}
