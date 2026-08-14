package com.teslamate.query.dto;

import java.time.Instant;

public record UpdateDto(
        Long updateId,
        Long carId,
        Instant startDate,
        Instant endDate,
        String version
) {
}
