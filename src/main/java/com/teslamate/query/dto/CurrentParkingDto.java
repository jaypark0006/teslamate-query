package com.teslamate.query.dto;

import java.math.BigDecimal;

public record CurrentParkingDto(
        Long carId,
        Long durationMin,
        BigDecimal outsideTempC
) {
}
