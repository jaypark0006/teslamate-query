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
    }

    @Test
    void rangeModeValidation() {
        assertEquals("ideal", support.rangeMode(null, "ideal"));
        assertEquals("rated", support.rangeMode("RATED", null));
        assertThrows(BadRequestException.class, () -> support.rangeMode("foo", null));
    }

    @Test
    void parseInstantAndRequireRange() {
        Instant a = support.parseInstant("2024-01-01T00:00:00Z", "from");
        Instant b = support.parseInstant("2024-02-01T00:00:00Z", "to");
        assertNotNull(a);
        support.requireTimeRange(a, b);
        assertThrows(BadRequestException.class, () -> support.requireTimeRange(b, a));
        assertThrows(BadRequestException.class, () -> support.requireTimeRange(null, b));
    }
}
