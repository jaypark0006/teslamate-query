package com.teslamate.query.dto;

import java.time.Instant;
import java.util.List;

public record BatteryHealthDto(
        Long carId,
        String rangeMode,
        Double currentUsableCapacityKwh,
        Double maxObservedCapacityKwh,
        Double degradationPercent,
        List<CapacityPoint> capacityOverTime
) {
    public record CapacityPoint(
            Instant date,
            Double odometerKm,
            Double capacityKwh,
            Double fullRangeKm
    ) {
    }
}
