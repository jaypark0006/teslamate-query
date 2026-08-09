package com.teslamate.query.dto;

import java.time.Instant;

public record TimelineEventDto(
        String type,
        Long refId,
        Instant startDate,
        Instant endDate,
        String label,
        Double value
) {
}
