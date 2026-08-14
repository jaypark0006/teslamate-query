package com.teslamate.query.dto;

import java.time.Instant;

public record StateDto(
        Long stateId,
        Long carId,
        String state,
        Instant startDate,
        Instant endDate,
        Long durationSeconds
) {
}
