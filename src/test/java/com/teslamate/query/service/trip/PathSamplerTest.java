package com.teslamate.query.service.trip;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathSamplerTest {

    @Test
    void keepsEndsAndCaps() {
        List<Long> ids = PathSampler.sampleIds(1000, 1999, 11);
        assertEquals(1000L, ids.getFirst());
        assertEquals(1999L, ids.getLast());
        assertEquals(11, ids.size());
    }

    @Test
    void singleId() {
        assertEquals(List.of(5L), PathSampler.sampleIds(5, 5, 20));
    }

    @Test
    void swappedRangeStillWorks() {
        List<Long> ids = PathSampler.sampleIds(50, 10, 5);
        assertEquals(10L, ids.getFirst());
        assertEquals(50L, ids.getLast());
        assertTrue(ids.size() <= 5);
    }
}
