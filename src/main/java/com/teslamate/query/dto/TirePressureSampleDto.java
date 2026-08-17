package com.teslamate.query.dto;

import java.time.Instant;

public record TirePressureSampleDto(
        Instant date,
        TirePressureDto tirePressure
) {
}
