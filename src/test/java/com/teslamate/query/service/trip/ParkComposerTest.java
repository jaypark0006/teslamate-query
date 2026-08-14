package com.teslamate.query.service.trip;

import com.teslamate.query.entity.DriveEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParkComposerTest {

    @Test
    void parkBetweenTwoSeparatedDrives() {
        Instant t0 = Instant.parse("2025-10-01T08:00:00Z");
        Instant t1 = Instant.parse("2025-10-01T09:00:00Z");
        Instant t2 = Instant.parse("2025-10-01T12:00:00Z");
        Instant t3 = Instant.parse("2025-10-01T13:00:00Z");
        List<ParkGap> parks = ParkComposer.compose(
                List.of(drive(1, t0, t1, 10L), drive(2, t2, t3, 20L)),
                Instant.parse("2025-10-01T00:00:00Z"),
                Instant.parse("2025-10-02T00:00:00Z"),
                t3,
                15, 15);
        assertEquals(1, parks.size());
        assertEquals(t1, parks.getFirst().start());
        assertEquals(t2, parks.getFirst().end());
        assertEquals(180.0, parks.getFirst().durationMin());
        assertEquals(10L, parks.getFirst().endPositionId());
    }

    @Test
    void shortGapMergesIntoOneCluster() {
        Instant t0 = Instant.parse("2025-10-01T08:00:00Z");
        Instant t1 = Instant.parse("2025-10-01T09:00:00Z");
        Instant t2 = Instant.parse("2025-10-01T09:05:00Z");
        Instant t3 = Instant.parse("2025-10-01T10:00:00Z");
        List<ParkGap> parks = ParkComposer.compose(
                List.of(drive(1, t0, t1, 10L), drive(2, t2, t3, 20L)),
                Instant.parse("2025-10-01T00:00:00Z"),
                Instant.parse("2025-10-02T00:00:00Z"),
                Instant.parse("2025-10-02T00:00:00Z"),
                15, 15);
        assertTrue(parks.stream().noneMatch(p -> p.start().equals(t1) && p.end().equals(t2)));
    }

    @Test
    void shortParkFiltered() {
        Instant t0 = Instant.parse("2025-10-01T08:00:00Z");
        Instant t1 = Instant.parse("2025-10-01T09:00:00Z");
        Instant t2 = Instant.parse("2025-10-01T09:10:00Z");
        Instant t3 = Instant.parse("2025-10-01T10:00:00Z");
        List<ParkGap> parks = ParkComposer.compose(
                List.of(drive(1, t0, t1, 10L), drive(2, t2, t3, 20L)),
                Instant.parse("2025-10-01T00:00:00Z"),
                Instant.parse("2025-10-02T00:00:00Z"),
                t3,
                5, 15);
        assertTrue(parks.isEmpty());
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
}
