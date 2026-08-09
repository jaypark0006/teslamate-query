package com.teslamate.query.dto;

import java.time.Instant;
import java.util.List;

public record ProjectedRangeDto(
        Long carId,
        String rangeMode,
        List<Point> points
) {
    public record Point(
            Instant date,
            Double odometerKm,
            Double projectedFullRangeKm,
            Integer batteryLevel,
            Double outsideTempC
    ) {
    }
}
