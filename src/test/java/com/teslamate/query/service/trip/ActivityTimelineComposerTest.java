package com.teslamate.query.service.trip;

import com.teslamate.query.dto.TimelineKind;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityTimelineComposerTest {

    private static final Instant FROM = Instant.parse("2026-08-16T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-08-16T12:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-16T18:00:00Z");

    @Test
    void driveChargeParkDrive() {
        List<ActivitySpan> spans = ActivityTimelineComposer.compose(
                List.of(
                        drive(1, "02:00:00", "03:00:00", 10L),
                        drive(2, "07:00:00", "08:00:00", 20L)),
                List.of(charge(9, "03:00:00", "04:00:00", 11L)),
                FROM, TO, NOW, 15, 1L, 0L);

        assertEquals(List.of(
                        TimelineKind.PARK, TimelineKind.DRIVE, TimelineKind.CHARGE,
                        TimelineKind.PARK, TimelineKind.DRIVE, TimelineKind.PARK),
                spans.stream().map(ActivitySpan::kind).toList());
        assertEquals(Instant.parse("2026-08-16T03:00:00Z"), spans.get(2).start());
        assertEquals(Instant.parse("2026-08-16T04:00:00Z"), spans.get(3).start());
        assertEquals(Instant.parse("2026-08-16T07:00:00Z"), spans.get(3).end());
        assertEquals(180.0, spans.get(3).durationMin());
        assertEquals(11L, spans.get(3).locationPositionId());
        assertEquals(1L, spans.get(3).afterDriveId());
    }

    @Test
    void chargeImmediatelyAfterDriveHasNoParkBetween() {
        List<ActivitySpan> spans = ActivityTimelineComposer.compose(
                List.of(drive(1, "02:00:00", "03:00:00", 10L)),
                List.of(charge(9, "03:00:00", "04:30:00", 10L)),
                FROM, TO, NOW, 15, null, null);
        assertEquals(List.of(TimelineKind.PARK, TimelineKind.DRIVE, TimelineKind.CHARGE, TimelineKind.PARK),
                spans.stream().map(ActivitySpan::kind).toList());
    }

    @Test
    void shortGapIsNotAPark() {
        List<ActivitySpan> spans = ActivityTimelineComposer.compose(
                List.of(
                        drive(1, "02:00:00", "03:00:00", 10L),
                        drive(2, "03:08:00", "04:00:00", 20L)),
                List.of(),
                FROM, TO, NOW, 15, null, null);
        assertTrue(spans.stream().noneMatch(s -> s.kind() == TimelineKind.PARK && s.durationMin() < 15));
        assertEquals(2, spans.stream().filter(s -> s.kind() == TimelineKind.DRIVE).count());
    }

    @Test
    void clipsToWindow() {
        List<ActivitySpan> spans = ActivityTimelineComposer.compose(
                List.of(drive(1, Instant.parse("2026-08-15T22:00:00Z"), Instant.parse("2026-08-16T01:00:00Z"), 10L)),
                List.of(),
                FROM, TO, NOW, 15, null, null);
        assertEquals(FROM, spans.getFirst().start());
        assertEquals(Instant.parse("2026-08-16T01:00:00Z"), spans.getFirst().end());
    }

    @Test
    void openDriveHasNoTrailingPark() {
        Instant now = Instant.parse("2026-08-16T04:00:00Z");
        List<ActivitySpan> spans = ActivityTimelineComposer.compose(
                List.of(drive(1, Instant.parse("2026-08-16T03:00:00Z"), null, 10L)),
                List.of(),
                FROM, TO, now, 15, null, null);
        assertEquals(TimelineKind.DRIVE, spans.getLast().kind());
        assertEquals(now, spans.getLast().end());
        assertTrue(spans.stream().noneMatch(s -> s.kind() == TimelineKind.PARK && s.start().isAfter(now.minusSeconds(1))));
    }

    @Test
    void trailingParkStopsAtNowWhenWindowIsLive() {
        Instant now = Instant.parse("2026-08-16T10:00:00Z");
        List<ActivitySpan> spans = ActivityTimelineComposer.compose(
                List.of(drive(1, "02:00:00", "03:00:00", 10L)),
                List.of(),
                FROM, TO, now, 15, null, null);
        ActivitySpan last = spans.getLast();
        assertEquals(TimelineKind.PARK, last.kind());
        assertEquals(now, last.end());
    }

    @Test
    void chargeSplitsALongPark() {
        List<ActivitySpan> spans = ActivityTimelineComposer.compose(
                List.of(
                        drive(1, "01:00:00", "02:00:00", 10L),
                        drive(2, "08:00:00", "09:00:00", 20L)),
                List.of(charge(9, "04:00:00", "05:00:00", 11L)),
                FROM, TO, NOW, 15, null, null);
        assertEquals(List.of(
                        TimelineKind.PARK, TimelineKind.DRIVE, TimelineKind.PARK, TimelineKind.CHARGE,
                        TimelineKind.PARK, TimelineKind.DRIVE, TimelineKind.PARK),
                spans.stream().map(ActivitySpan::kind).toList());
        assertEquals(120.0, spans.get(2).durationMin());
        assertEquals(180.0, spans.get(4).durationMin());
    }

    @Test
    void emptyWindow() {
        assertTrue(ActivityTimelineComposer.compose(List.of(), List.of(), FROM, TO, NOW, 15, null, null).isEmpty());
    }

    @Test
    void seedPositionUsedForLeadingPark() {
        List<ActivitySpan> spans = ActivityTimelineComposer.compose(
                List.of(drive(1, "02:00:00", "03:00:00", 10L)),
                List.of(),
                FROM, TO, NOW, 15, 99L, 7L);
        assertEquals(TimelineKind.PARK, spans.getFirst().kind());
        assertEquals(99L, spans.getFirst().locationPositionId());
        assertEquals(7L, spans.getFirst().afterDriveId());
        assertNull(spans.getFirst().sourceId());
    }

    private static DriveEntity drive(long id, String start, String end, Long endPos) {
        return drive(id, t(start), t(end), endPos);
    }

    private static DriveEntity drive(long id, Instant start, Instant end, Long endPos) {
        return new DriveEntity(
                id, 1L, start, end,
                null, null, null, null, null,
                null, null, null, null,
                null, null,
                10.0, 60, null, null,
                null, endPos,
                null, null, null, null);
    }

    private static ChargingProcessEntity charge(long id, String start, String end, Long pos) {
        return new ChargingProcessEntity(
                id, 1L, t(start), t(end),
                new BigDecimal("12.5"), new BigDecimal("13.0"),
                null, null, null, null,
                30, 60, 60, null, null,
                pos, null, null);
    }

    private static Instant t(String hhmmss) {
        return Instant.parse("2026-08-16T" + hhmmss + "Z");
    }
}
