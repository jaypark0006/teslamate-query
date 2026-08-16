package com.teslamate.query.service.trip;

import com.teslamate.query.entity.PositionPathPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathSimplifyTest {

    @Test
    void straightLineCollapsesToEnds() {
        List<PositionPathPoint> line = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            line.add(pt(106.0 + i * 0.00005, 29.5));
        }
        List<PositionPathPoint> out = PathSimplify.douglasPeucker(line, 8);
        assertEquals(2, out.size());
        assertEquals(line.getFirst().longitude(), out.getFirst().longitude());
        assertEquals(line.getLast().longitude(), out.getLast().longitude());
    }

    @Test
    void cornerIsKept() {
        List<PositionPathPoint> l = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            l.add(pt(106.0 + i * 0.0001, 29.5));
        }
        for (int i = 1; i <= 20; i++) {
            l.add(pt(106.002, 29.5 + i * 0.0001));
        }
        List<PositionPathPoint> out = PathSimplify.douglasPeucker(l, 8);
        assertTrue(out.size() >= 3);
        assertTrue(out.size() < l.size());
        boolean hasCorner = out.stream().anyMatch(p ->
                Math.abs(p.longitude().doubleValue() - 106.002) < 1e-6
                        && Math.abs(p.latitude().doubleValue() - 29.5) < 1e-6);
        assertTrue(hasCorner);
    }

    @Test
    void shortListUnchanged() {
        List<PositionPathPoint> two = List.of(pt(106.0, 29.5), pt(106.1, 29.6));
        assertEquals(2, PathSimplify.douglasPeucker(two, 12).size());
        assertTrue(PathSimplify.douglasPeucker(List.of(), 12).isEmpty());
    }

    @Test
    void widerWindowUsesCoarserLod() {
        assertEquals(8.0, PathSimplify.epsilonMeters(86_400));
        assertEquals(25.0, PathSimplify.epsilonMeters(7 * 86_400));
        assertEquals(80.0, PathSimplify.epsilonMeters(30 * 86_400));
        assertEquals(5, PathSimplify.sampleBucketSeconds(86_400, 8));
        assertEquals(2, PathSimplify.sampleBucketSeconds(86_400, 1));
        assertTrue(PathSimplify.endpointsOnly(60 * 86_400, 641));
        assertFalse(PathSimplify.endpointsOnly(86_400, 8));
        assertEquals(12, PathSimplify.maxPointsPerDrive(60 * 86_400, 641));
    }

    @Test
    void capKeepsEndsAndShrinks() {
        List<PositionPathPoint> line = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            line.add(pt(106.0 + i * 0.001, 29.5));
        }
        List<PositionPathPoint> out = PathSimplify.cap(line, 10);
        assertEquals(10, out.size());
        assertEquals(line.getFirst().longitude(), out.getFirst().longitude());
        assertEquals(line.getLast().longitude(), out.getLast().longitude());
    }

    private static PositionPathPoint pt(double lon, double lat) {
        return new PositionPathPoint(1L, Instant.parse("2026-08-16T00:00:00Z"),
                BigDecimal.valueOf(lon), BigDecimal.valueOf(lat));
    }
}
