package com.teslamate.query.dto;

import java.util.List;

public record EfficiencyStatsDto(
        Long carId,
        String rangeMode,
        Double netConsumptionWhPerKm,
        Double grossConsumptionWhPerKm,
        Double carEfficiency,
        Double totalDistanceKm,
        List<TempBucket> byOutsideTemp
) {
    public record TempBucket(
            Integer tempC,
            long driveCount,
            double distanceKm,
            Double consumptionWhPerKm
    ) {
    }
}
