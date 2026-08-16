package com.teslamate.query.service;

import com.teslamate.query.dto.DayGridCellDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripViewServiceTest {

    @Test
    void focusKindCodeReadsGridClick() {
        assertEquals(0, TripViewService.focusKindCode(null));
        assertEquals(2, TripViewService.focusKindCode("2"));
        assertEquals(2, TripViewService.focusKindCode("DRIVE"));
        assertEquals(2, TripViewService.focusKindCode("Drive #18432"));
        assertEquals(3, TripViewService.focusKindCode("charge"));
        assertEquals(3, TripViewService.focusKindCode("Charge #99"));
        assertEquals(2, TripViewService.focusKindCode(String.valueOf(
                DayGridCellDto.hoverCode(DayGridCellDto.DRIVE, 5155))));
        assertEquals(2, TripViewService.focusKindCode(String.valueOf(
                DayGridCellDto.hoverCode(DayGridCellDto.HIGHLIGHT_DRIVE, 5155))));
        assertEquals(3, TripViewService.focusKindCode(String.valueOf(
                DayGridCellDto.hoverCode(DayGridCellDto.CHARGE, 99))));
    }

    @Test
    void coerceInstantReadsGrafanaDates() {
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                TripViewService.coerceInstant("2026-08-16T04:12:00Z"));
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                TripViewService.coerceInstant("2026-08-16T12:12:00+08:00"));
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                TripViewService.coerceInstant("2026-08-16T12:12:00 08:00"));
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                TripViewService.coerceInstant(String.valueOf(Instant.parse("2026-08-16T04:12:00Z").getEpochSecond())));
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                TripViewService.coerceInstant(String.valueOf(Instant.parse("2026-08-16T04:12:00Z").toEpochMilli())));
    }

    @Test
    void parseKindsDefaultAndFilter() {
        assertEquals(Set.of("drive", "charge", "park"), TripViewService.parseKinds(null));
        assertEquals(Set.of("drive", "charge", "park"), TripViewService.parseKinds("${kinds}"));
        assertEquals(Set.of("drive"), TripViewService.parseKinds("drive"));
        assertEquals(Set.of("charge", "park"), TripViewService.parseKinds("charge,park"));
    }

    @Test
    void parkDurationIsDashWhileStillParked() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        assertTrue(TripViewService.liveWindow(now, now));
        assertTrue(TripViewService.openEnded(now, now));
        assertEquals("-", TripViewService.parkDurationLabel(180, true));
        assertEquals("3h", TripViewService.parkDurationLabel(180, false));
        assertEquals("38 min", TripViewService.parkDurationLabel(38, false));
        assertFalse(TripViewService.liveWindow(Instant.parse("2026-08-10T00:00:00Z"), now));
    }
}
