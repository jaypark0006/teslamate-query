package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RecentChargeDto(
        Long id,
        Instant startDate,
        Instant endDate,
        Integer durationMin,
        BigDecimal energyAddedKwh,
        ChargeType chargeType,
        BigDecimal startRangeKm,
        BigDecimal endRangeKm,
        Integer startSocPercent,
        Integer endSocPercent
) {
}
