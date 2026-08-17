package com.teslamate.query.service;

import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.dto.DayGridCellDto;
import com.teslamate.query.dto.TimelineKind;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripViewServiceTest {

    @Test
    void timelineKindReadsGrafanaJunk() {
        assertEquals(Optional.empty(), TimelineKind.parse(null));
        assertEquals(Optional.of(TimelineKind.DRIVE), TimelineKind.parse("2"));
        assertEquals(Optional.of(TimelineKind.DRIVE), TimelineKind.parse("DRIVE"));
        assertEquals(Optional.of(TimelineKind.DRIVE), TimelineKind.parse("Drive #18432"));
        assertEquals(Optional.of(TimelineKind.CHARGE), TimelineKind.parse("charge"));
        assertEquals(Optional.of(TimelineKind.CHARGE), TimelineKind.parse("Charge #99"));
        assertEquals(Optional.of(TimelineKind.DRIVE), TimelineKind.parse(String.valueOf(
                DayGridCellDto.hoverCode(DayGridCellDto.DRIVE, 5155))));
        assertEquals(Optional.of(TimelineKind.DRIVE), TimelineKind.parse(String.valueOf(
                DayGridCellDto.hoverCode(DayGridCellDto.HIGHLIGHT_DRIVE, 5155))));
        assertEquals(Optional.of(TimelineKind.CHARGE), TimelineKind.parse(String.valueOf(
                DayGridCellDto.hoverCode(DayGridCellDto.CHARGE, 99))));
    }

    @Test
    void layersParseCommaList() {
        assertEquals(EnumSet.allOf(TimelineKind.class), TimelineKind.parseLayers(null));
        assertEquals(EnumSet.allOf(TimelineKind.class), TimelineKind.parseLayers("${kinds}"));
        assertEquals(Set.of(TimelineKind.DRIVE), TimelineKind.parseLayers("drive"));
        assertEquals(Set.of(TimelineKind.CHARGE, TimelineKind.PARK), TimelineKind.parseLayers("charge,park"));
    }

    @Test
    void optionalInstantReadsGrafanaDates() {
        QuerySupport support = new QuerySupport(new QueryProperties());
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                support.optionalInstant("2026-08-16T04:12:00Z"));
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                support.optionalInstant("2026-08-16T12:12:00+08:00"));
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                support.optionalInstant("2026-08-16T12:12:00 08:00"));
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                support.optionalInstant(String.valueOf(Instant.parse("2026-08-16T04:12:00Z").getEpochSecond())));
        assertEquals(Instant.parse("2026-08-16T04:12:00Z"),
                support.optionalInstant(String.valueOf(Instant.parse("2026-08-16T04:12:00Z").toEpochMilli())));
        assertEquals(5 * 60 + 40, support.clockMinutes("05:40", 0));
        assertEquals(5 * 60 + 40, support.clockMinutes("5:40", 0));
        assertEquals(5 * 60 + 30, support.clockMinutes("-", 5 * 60 + 30));
        assertEquals(null, support.optionalInstant("1970-01-01T00:00:00.008Z"));
        assertTrue(QuerySupport.plausibleTrip(Instant.parse("2024-08-16T10:00:00Z")));
        assertFalse(QuerySupport.plausibleTrip(Instant.parse("1970-01-01T00:00:00.008Z")));
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

    @Test
    void sealedWindowIsYesterdayNotToday() {
        Instant now = Instant.parse("2026-08-16T15:00:00Z");
        java.time.ZoneId sh = java.time.ZoneId.of("Asia/Shanghai");
        assertTrue(TripViewService.sealedWindow(Instant.parse("2026-08-13T11:55:33.897Z"), now, sh));
        assertFalse(TripViewService.sealedWindow(Instant.parse("2026-08-16T11:00:00Z"), now, sh));
        assertFalse(TripViewService.sealedWindow(now, now, sh));
        assertTrue(TripViewService.sealedDrive(
                new com.teslamate.query.entity.DriveEntity(
                        5186L, 1L,
                        Instant.parse("2026-08-13T05:48:54Z").atOffset(ZoneOffset.UTC).toLocalDateTime(),
                        Instant.parse("2026-08-13T06:13:40Z").atOffset(ZoneOffset.UTC).toLocalDateTime(),
                        null, null, null, null, null,
                        null, null, null, null,
                        null, null,
                        15.2, 25, null, null,
                        1L, 2L,
                        null, null, null, null),
                now, sh));
    }
}
