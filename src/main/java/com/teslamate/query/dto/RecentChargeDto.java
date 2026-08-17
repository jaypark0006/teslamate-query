package com.teslamate.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecentChargeDto(
        Long id,
        List<Long> chargingProcessIds,
        Integer mergedCount,
        Instant startDate,
        Instant endDate,
        Integer durationMin,
        BigDecimal energyAddedKwh,
        ChargeType chargeType,
        BigDecimal startRangeKm,
        BigDecimal endRangeKm,
        Integer startSocPercent,
        Integer endSocPercent,
        Double efficiencyPercent,
        Double avgPowerKw,
        Double power20to80Kw,
        Double power80toEndKw,
        String power20to80Label,
        String power80toEndLabel,
        BigDecimal energyUsedKwh,
        BigDecimal cost,
        Long positionId,
        Long addressId,
        Long geofenceId
) {
}
