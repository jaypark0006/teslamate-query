package com.teslamate.query.service.trip;

import com.teslamate.query.entity.PositionPathPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathGeometryTest {

    @Test
    void decimateKeepsEndsAndCapsSize() {
        List<PositionPathPoint> pts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            pts.add(new PositionPathPoint(1L, Instant.parse("2026-08-16T00:00:00Z").plusSeconds(i),
                    BigDecimal.valueOf(106 + i * 0.0001), BigDecimal.valueOf(29.5)));
        }
        List<PositionPathPoint> out = PathGeometry.decimate(pts, 11);
        assertTrue(out.size() <= 12);
        assertEquals(pts.getFirst().longitude(), out.getFirst().longitude());
        assertEquals(pts.getLast().longitude(), out.getLast().longitude());
    }

    @Test
    void eastwardBearingIsAbout90() {
        Double b = PathGeometry.bearingDeg(106.0, 29.5, 106.1, 29.5);
        assertEquals(90.0, b, 1.0);
    }

    @Test
    void samePointHasNoBearing() {
        assertNull(PathGeometry.bearingDeg(106.0, 29.5, 106.0, 29.5));
    }

    @Test
    void chevronHasThreeVertices() {
        List<List<BigDecimal>> line = PathGeometry.chevron(
                BigDecimal.valueOf(106.46), BigDecimal.valueOf(29.52), 90.0, 20.0);
        assertEquals(3, line.size());
        assertEquals(2, line.get(1).size());
    }

    @Test
    void pathBucketClamped() {
        assertEquals(15, PathGeometry.pathBucketSeconds(1_000));
        assertEquals(43, PathGeometry.pathBucketSeconds(86_400));
        assertEquals(180, PathGeometry.pathBucketSeconds(864_000));
    }
}
