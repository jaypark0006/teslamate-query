package com.teslamate.query.dto;

import java.math.BigDecimal;

public record GeofenceDto(
        Long id,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer radius,
        String billingType,
        BigDecimal costPerUnit,
        BigDecimal sessionFee
) {
}
