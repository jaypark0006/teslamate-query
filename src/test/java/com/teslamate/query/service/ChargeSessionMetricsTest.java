package com.teslamate.query.service;

import com.teslamate.query.entity.ChargeEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChargeSessionMetricsTest {

    @Test
    void efficiencyAndAvgPower() {
        assertEquals(95.4, ChargeSessionMetrics.efficiencyPercent(
                new BigDecimal("31.72"), new BigDecimal("33.26")));
        assertEquals(48.8, ChargeSessionMetrics.avgPowerKw(new BigDecimal("31.72"), 39));
        assertNull(ChargeSessionMetrics.avgPowerKw(new BigDecimal("10"), 0));
    }

    @Test
    void bandUsesEnergyOverTime() {
        List<ChargeEntity> samples = List.of(
                sample(20, "10:00:00", "0"),
                sample(50, "10:10:00", "10"),
                sample(80, "10:20:00", "20"),
                sample(95, "10:40:00", "28"));
        var full = ChargeSessionMetrics.band20to80(samples);
        assertEquals(60.0, full.kw());
        assertEquals("60", full.label());
        assertEquals(24.0, ChargeSessionMetrics.band80toEndKw(samples));
    }

    @Test
    void partialBandIsMarked() {
        List<ChargeEntity> samples = List.of(
                sample(34, "23:20:00", "0"),
                sample(50, "23:30:00", "8"),
                sample(61, "23:39:00", "15.2"));
        var band = ChargeSessionMetrics.band20to80(samples);
        assertEquals(48.0, band.kw());
        assertEquals("~48 (34–61%)", band.label());
        assertNull(ChargeSessionMetrics.band80toEnd(samples));
    }

    private static ChargeEntity sample(int soc, String time, String added) {
        return new ChargeEntity(
                1L, 1L, Instant.parse("2026-08-01T" + time + "Z"),
                soc, soc, new BigDecimal(added),
                null, null, null, null,
                true, "Gb", "GB_DC",
                null, null, null, false);
    }
}
