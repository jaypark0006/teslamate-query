package com.teslamate.query.service;

import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.entity.ChargeEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EntityMapperTest {

    @Test
    void exposesAcInputOnlyWhenTheSampleContainsMeaningfulAcValues() {
        var ac = EntityMapper.toChargeDto(
                charge(false, null, "IEC", 230, 16), DisplayUnits.METRIC);

        assertEquals(230, ac.acVoltage());
        assertEquals(16, ac.acCurrent());
    }

    @Test
    void hidesDcVoltageAndCurrentPlaceholders() {
        var dc = EntityMapper.toChargeDto(
                charge(true, "Gb", "GB_DC", 2, 0), DisplayUnits.METRIC);

        assertNull(dc.acVoltage());
        assertNull(dc.acCurrent());
        assertEquals(2, dc.chargerVoltage());
        assertEquals(0, dc.chargerActualCurrent());
    }

    private static ChargeEntity charge(
            boolean fast,
            String fastType,
            String cable,
            int voltage,
            int current
    ) {
        return new ChargeEntity(
                1L, 367L, LocalDateTime.of(2026, 8, 16, 22, 10),
                37, 37, null, 80, voltage, current, null,
                fast, fastType, cable, null, null, null, false);
    }
}
