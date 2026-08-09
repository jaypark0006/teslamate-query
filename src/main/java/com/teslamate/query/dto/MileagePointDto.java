package com.teslamate.query.dto;

import java.time.Instant;

public record MileagePointDto(
        Instant date,
        Double odometerKm
) {
}
