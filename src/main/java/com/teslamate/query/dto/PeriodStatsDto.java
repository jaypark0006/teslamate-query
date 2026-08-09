package com.teslamate.query.dto;

import java.util.List;

public record PeriodStatsDto(
        Long carId,
        String period,
        List<Row> rows
) {
    public record Row(
            String bucket,
            long drives,
            double driveDistanceKm,
            Double driveConsumptionKwh,
            long charges,
            double chargeEnergyAddedKwh,
            Double chargeCost
    ) {
    }
}
