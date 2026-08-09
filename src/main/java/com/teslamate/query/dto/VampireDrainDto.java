package com.teslamate.query.dto;

import java.time.Instant;
import java.util.List;

public record VampireDrainDto(
        Long carId,
        Instant from,
        Instant to,
        List<Segment> segments,
        Double totalRangeLossKm,
        Double avgLossPerHourKm
) {
    public record Segment(
            Instant startDate,
            Instant endDate,
            Double hours,
            Double startRangeKm,
            Double endRangeKm,
            Double rangeLossKm,
            Double lossPerHourKm,
            String startAddress,
            String endAddress
    ) {
    }
}
