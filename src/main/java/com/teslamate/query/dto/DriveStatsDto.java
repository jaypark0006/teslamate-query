package com.teslamate.query.dto;

import java.util.List;

public record DriveStatsDto(
        Long carId,
        String groupBy,
        Summary summary,
        List<Bucket> buckets
) {
    public record Summary(
            long driveCount,
            double totalDistanceKm,
            double totalDurationMin,
            Double avgDistanceKm,
            Double medianDistanceKm,
            Integer maxSpeed,
            Double totalConsumptionKwh,
            Double avgConsumptionWhPerKm
    ) {
    }

    public record Bucket(
            String period,
            long driveCount,
            double distanceKm,
            double durationMin,
            Double consumptionKwh,
            Double avgSpeedKmh
    ) {
    }
}
