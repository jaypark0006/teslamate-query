package com.teslamate.query.service.trip;

import com.teslamate.query.entity.DriveEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriveOutingComposerTest {

    @Test
    void zeroGapKeepsEachDrive() {
        List<List<DriveEntity>> out = DriveOutingComposer.newestOutings(
                List.of(drive(1, "08:00:00", "08:10:00"), drive(2, "08:12:00", "08:20:00")),
                0, 5);
        assertEquals(2, out.size());
        assertEquals(2L, out.getFirst().getFirst().id());
        assertEquals(1, out.getFirst().size());
    }

    @Test
    void shortGapMergesNewestFirst() {
        List<List<DriveEntity>> out = DriveOutingComposer.newestOutings(
                List.of(
                        drive(1, "08:00:00", "08:10:00"),
                        drive(2, "08:12:00", "08:20:00"),
                        drive(3, "12:00:00", "12:30:00")),
                5, 5);
        assertEquals(2, out.size());
        assertEquals(3L, out.getFirst().getFirst().id());
        assertEquals(List.of(1L, 2L), out.get(1).stream().map(DriveEntity::id).toList());
    }

    @Test
    void limitTakesNewestClusters() {
        List<List<DriveEntity>> out = DriveOutingComposer.newestOutings(
                List.of(
                        drive(1, "08:00:00", "08:10:00"),
                        drive(2, "10:00:00", "10:10:00"),
                        drive(3, "12:00:00", "12:10:00")),
                0, 2);
        assertEquals(2, out.size());
        assertEquals(3L, out.getFirst().getFirst().id());
        assertEquals(2L, out.get(1).getFirst().id());
    }

    private static DriveEntity drive(long id, String start, String end) {
        return new DriveEntity(
                id, 1L,
                Instant.parse("2026-08-13T" + start + "Z"),
                Instant.parse("2026-08-13T" + end + "Z"),
                null, null, null, null, null,
                null, null, null, null,
                0.0, 0.0, 1.0, 10, null, null,
                null, null, null, null, null, null);
    }
}
