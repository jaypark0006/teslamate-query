package com.teslamate.query.dto;

import java.time.Instant;
import java.util.List;

public record RecentDriveDto(
        Long id,
        List<Long> driveIds,
        Instant startDate,
        Instant endDate,
        Integer durationMin,
        Double distanceKm,
        Double energyUsedKwh,
        Double rangeUsedKm,
        Double startRangeKm,
        Double endRangeKm,
        Integer startSocPercent,
        Integer endSocPercent,
        Double avgSpeedKmh
) {
}
