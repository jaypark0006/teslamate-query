package com.teslamate.query.service.trip;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PathGeometryTest {

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
}
