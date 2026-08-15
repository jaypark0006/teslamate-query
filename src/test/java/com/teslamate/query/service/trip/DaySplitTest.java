package com.teslamate.query.service.trip;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DaySplitTest {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    @Test
    void overnightParkKeepsStartDayAndClipsClockToMidnight() {
        // 22:00 Aug 10 CST → 08:00 Aug 11 CST — still one park on Aug 10
        assertEquals("2026-08-10", DaySplit.dayLabel(Instant.parse("2026-08-10T14:00:00Z"), SHANGHAI));
        Instant[] c = DaySplit.clockRange(
                Instant.parse("2026-08-10T14:00:00Z"),
                Instant.parse("2026-08-11T00:00:00Z"),
                SHANGHAI, LocalDate.of(2000, 1, 1), 0);
        assertEquals(Instant.parse("2000-01-01T14:00:00Z"), c[0]);
        assertEquals(Instant.parse("2000-01-01T16:00:00Z"), c[1]);
    }

    @Test
    void clockRangeMapsOntoDummyDay() {
        Instant[] c = DaySplit.clockRange(
                Instant.parse("2026-08-10T14:00:00Z"),
                Instant.parse("2026-08-10T16:00:00Z"),
                SHANGHAI, LocalDate.of(2000, 1, 1), 0);
        assertEquals(Instant.parse("2000-01-01T14:00:00Z"), c[0]);
        assertEquals(Instant.parse("2000-01-01T16:00:00Z"), c[1]);
    }

    @Test
    void shortDriveGetsAMinimumClockWidth() {
        Instant[] c = DaySplit.clockRange(
                Instant.parse("2026-08-13T02:00:00Z"),
                Instant.parse("2026-08-13T02:03:00Z"),
                SHANGHAI, LocalDate.of(2000, 1, 1), 12);
        assertEquals(12, java.time.Duration.between(c[0], c[1]).toMinutes());
    }

    @Test
    void adjacentLocalDaysGetOppositeBands() {
        int a = DaySplit.dayBand(Instant.parse("2026-08-10T16:00:00Z"), SHANGHAI);
        int b = DaySplit.dayBand(Instant.parse("2026-08-11T16:00:00Z"), SHANGHAI);
        assertEquals(1, Math.abs(a - b));
    }
}
