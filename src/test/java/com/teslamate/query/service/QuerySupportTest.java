package com.teslamate.query.service;

import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class QuerySupportTest {

    private QuerySupport support;

    @BeforeEach
    void setUp() {
        QueryProperties props = new QueryProperties();
        props.setDefaultPageSize(50);
        props.setMaxPageSize(500);
        support = new QuerySupport(props);
    }

    @Test
    void pageDefaultsAndClamps() {
        assertEquals(1, support.page(null));
        assertEquals(1, support.page(0));
        assertEquals(3, support.page(3));
        assertEquals(50, support.size(null));
        assertEquals(500, support.size(9999));
        assertEquals(100, support.offset(3, 50));
        assertEquals(5, support.recentLimit(null));
        assertEquals(10, support.recentLimit(99));
        assertEquals(0, support.mergeGapMin((Integer) null));
        assertEquals(180, support.mergeGapMin(999));
        assertEquals(15, support.chargeMergeGapMin(null));
        assertEquals(0, support.chargeMergeGapMin(-1));
        assertEquals(180, support.chargeMergeGapMin(999));
        assertEquals(100, support.mergeDistanceM(null));
        assertEquals(0, support.mergeDistanceM(-1));
        assertEquals(1_000, support.mergeDistanceM(9_999));
        assertEquals(0, support.mergeGapMin("Off"));
        assertEquals(0, support.mergeGapMin("${merge_gap}"));
        assertEquals(15, support.mergeGapMin("15 min"));
        assertEquals(10, support.minParkMin((String) null));
        assertEquals(10, support.minParkMin("${min_park}"));
        assertEquals(15, support.minParkMin("15 min"));
        assertEquals("Asia/Shanghai", support.zone(null).getId());
        assertEquals("Asia/Shanghai", support.zone("browser").getId());
        assertEquals("UTC", support.zone("UTC").getId());
        assertEquals(0, support.dayStartHour(null));
        assertEquals(0, support.dayStartHour("${dayStartHour}"));
        assertEquals(4, support.dayStartHour("4"));
        assertEquals(4, support.dayStartHour("28"));
    }

    @Test
    void parseInstantAndRequireRange() {
        Instant a = support.parseInstant("2024-01-01T00:00:00Z", "from");
        Instant b = support.parseInstant("2024-02-01T00:00:00Z", "to");
        assertNotNull(a);
        Instant[] r = support.requireRange("2024-01-01T00:00:00Z", "2024-02-01T00:00:00Z");
        assertEquals(a, r[0]);
        assertThrows(BadRequestException.class, () -> support.requireRange("2024-02-01T00:00:00Z", "2024-01-01T00:00:00Z"));
        assertThrows(BadRequestException.class, () -> support.requireRange(null, "2024-01-01T00:00:00Z"));
    }
}
