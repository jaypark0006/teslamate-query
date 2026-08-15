package com.teslamate.query.dto;

import java.math.BigDecimal;

public record CurrentDriveDto(
        Long carId,
        Long driveId,
        Integer speed,
        Integer power,
        Double distanceKm,
        Long durationMin,
        BigDecimal rangeChangeKm
) {
}
