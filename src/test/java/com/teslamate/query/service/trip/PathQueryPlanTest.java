package com.teslamate.query.service.trip;

import com.teslamate.query.entity.DriveEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathQueryPlanTest {

    @Test
    void highlightKeepsFullPath() {
        PathQueryPlan plan = PathQueryPlan.of(List.of(drive(1, 60, 20.0)));
        assertEquals(1, plan.queries().size());
        assertEquals(0, plan.skipped());
        assertEquals(0, plan.queries().getFirst().bucketSec());
        assertEquals(PathQueryPlan.BUDGET, plan.queries().getFirst().cap());
        assertEquals(0, plan.epsilonM());
    }

    @Test
    void shortDriveKeepsFullPath() {
        PathQueryPlan plan = PathQueryPlan.of(List.of(drive(1, 8, 3.0)));
        assertEquals(0, plan.queries().getFirst().bucketSec());
        assertEquals(PathQueryPlan.BUDGET, plan.queries().getFirst().cap());
    }

    @Test
    void dayOfShortDrivesKeepsFullPath() {
        List<DriveEntity> drives = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            drives.add(drive(i, 10, 4.0));
        }
        PathQueryPlan plan = PathQueryPlan.of(drives);
        assertEquals(6, plan.queries().size());
        for (PathQueryPlan.Query q : plan.queries()) {
            assertEquals(0, q.bucketSec());
            assertEquals(PathQueryPlan.BUDGET, q.cap());
        }
        assertEquals(0, plan.epsilonM());
    }

    @Test
    void minuteHopsSkipPositionsSql() {
        PathQueryPlan plan = PathQueryPlan.of(List.of(
                drive(1, 1, 0.0),
                drive(2, 2, 0.2)));
        assertTrue(plan.queries().isEmpty());
        assertEquals(2, plan.skipped());
    }

    @Test
    void mixedWindowSkipsHopsAndSamplesTheLongRun() {
        List<DriveEntity> drives = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            drives.add(drive(i, 1, 0.1));
        }
        drives.add(drive(99, 45, 30.0));
        PathQueryPlan plan = PathQueryPlan.of(drives);
        assertEquals(20, plan.skipped());
        assertEquals(1, plan.queries().size());
        assertEquals(99L, plan.queries().getFirst().driveId());
        assertEquals(0, plan.queries().getFirst().bucketSec());
        assertEquals(PathQueryPlan.BUDGET, plan.queries().getFirst().cap());
    }

    @Test
    void dayMixKeepsShortDrivesFull() {
        List<DriveEntity> drives = List.of(
                drive(1, 10, 4.0),
                drive(2, 140, 180.0),
                drive(3, 8, 3.0));
        PathQueryPlan plan = PathQueryPlan.of(drives);
        assertEquals(0, bucketOf(plan, 1));
        assertEquals(0, bucketOf(plan, 3));
        assertTrue(bucketOf(plan, 2) >= 5, "long drive in a busy window still lods");
        assertTrue(plan.epsilonM() > 0);
    }

    @Test
    void manyMediumDrivesShareTheBudget() {
        List<DriveEntity> drives = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            drives.add(drive(i, 25, 12.0));
        }
        PathQueryPlan plan = PathQueryPlan.of(drives);
        assertEquals(100, plan.queries().size());
        assertEquals(0, plan.skipped());
        int bucket = plan.queries().getFirst().bucketSec();
        assertTrue(bucket >= 15 && bucket <= 60, "bucket=" + bucket);
        int cap = plan.queries().getFirst().cap();
        assertTrue(cap >= 6 && cap <= 80, "cap=" + cap);
        assertTrue(plan.batches().size() >= 100 / PathSimplify.PATH_BATCH);
    }

    private static int bucketOf(PathQueryPlan plan, long driveId) {
        return plan.queries().stream()
                .filter(q -> q.driveId() == driveId)
                .findFirst()
                .orElseThrow()
                .bucketSec();
    }

    private static DriveEntity drive(long id, int durationMin, double km) {
        Instant start = Instant.parse("2026-08-16T00:00:00Z");
        Instant end = start.plusSeconds(durationMin * 60L);
        return new DriveEntity(
                id, 1L, start, end,
                null, null, null, null, null,
                null, null, null, null,
                null, null,
                km, durationMin, null, null,
                1L, 2L,
                null, null, null, null);
    }
}
