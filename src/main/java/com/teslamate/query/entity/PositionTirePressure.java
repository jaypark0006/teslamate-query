package com.teslamate.query.entity;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PositionTirePressure(
        @ColumnName("date") LocalDateTime date,
        @ColumnName("tpms_pressure_fl") BigDecimal fl,
        @ColumnName("tpms_pressure_fr") BigDecimal fr,
        @ColumnName("tpms_pressure_rl") BigDecimal rl,
        @ColumnName("tpms_pressure_rr") BigDecimal rr
) {
}
