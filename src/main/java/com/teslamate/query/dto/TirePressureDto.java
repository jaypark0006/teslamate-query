package com.teslamate.query.dto;

import java.math.BigDecimal;

public record TirePressureDto(
        BigDecimal fl,
        BigDecimal fr,
        BigDecimal rl,
        BigDecimal rr
) {
}
