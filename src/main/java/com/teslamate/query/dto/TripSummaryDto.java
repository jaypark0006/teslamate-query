package com.teslamate.query.dto;

import java.time.Instant;
import java.util.List;

public record TripSummaryDto(
        Long carId,
        Instant from,
        Instant to,
        double driveDistanceKm,
        long driveCount,
        long chargeCount,
        Double chargeEnergyAddedKwh,
        Double chargeCost,
        Double netConsumptionWhPerKm,
        Double totalDurationMin,
        List<DriveDto> drives,
        List<ChargingProcessDto> charges
) {
}
