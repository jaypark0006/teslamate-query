package com.teslamate.query.service.trip;

import com.teslamate.query.dto.TimelineKind;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DaySplitTest {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    @Test
    void sameLocalDayStaysOnePiece() {
        ActivitySpan park = span("2026-08-13T02:00:00Z", "2026-08-13T08:00:00Z");
        List<ActivitySpan> out = DaySplit.splitByLocalDays(List.of(park), SHANGHAI);
        assertEquals(1, out.size());
        assertEquals(360.0, out.getFirst().durationMin());
    }

    @Test
    void shanghaiMidnightSplitsALongPark() {
        // 22:00 Aug 10 CST → 08:00 Aug 11 CST
        ActivitySpan park = span("2026-08-10T14:00:00Z", "2026-08-11T00:00:00Z");
        List<ActivitySpan> out = DaySplit.splitByLocalDays(List.of(park), SHANGHAI);
        assertEquals(2, out.size());
        assertEquals(Instant.parse("2026-08-10T16:00:00Z"), out.get(0).end());
        assertEquals(Instant.parse("2026-08-10T16:00:00Z"), out.get(1).start());
        assertEquals(120.0, out.get(0).durationMin());
        assertEquals(480.0, out.get(1).durationMin());
        assertEquals("2026-08-10", DaySplit.dayLabel(out.get(0).start(), SHANGHAI));
        assertEquals("2026-08-11", DaySplit.dayLabel(out.get(1).start(), SHANGHAI));
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

    private static ActivitySpan span(String start, String end) {
        Instant a = Instant.parse(start);
        Instant b = Instant.parse(end);
        double mins = java.time.Duration.between(a, b).toMinutes();
        return new ActivitySpan(TimelineKind.PARK, null, a, b, mins, 1L, 1L);
    }
}
