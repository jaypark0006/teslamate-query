package com.teslamate.query.dto;

import java.util.List;

public record ChargingStatsDto(
        Long carId,
        Summary summary,
        List<TypeBreakdown> byType,
        List<Station> topStations
) {
    public record Summary(
            long chargeCount,
            double energyAddedKwh,
            double energyUsedKwh,
            Double totalCost,
            Double costPerKwh,
            double totalDurationMin
    ) {
    }

    public record TypeBreakdown(
            String chargeType,
            long count,
            double energyAddedKwh,
            double durationMin,
            Double cost
    ) {
    }

    public record Station(
            String name,
            Long geofenceId,
            long count,
            double energyAddedKwh,
            Double cost
    ) {
    }
}
