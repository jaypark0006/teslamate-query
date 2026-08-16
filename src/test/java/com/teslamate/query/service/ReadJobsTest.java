package com.teslamate.query.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadJobsTest {

    @Test
    void bothReturnsEachSide() {
        ReadJobs.Pair<Integer, String> pair = ReadJobs.both(() -> 7, () -> "ok");
        assertEquals(7, pair.first());
        assertEquals("ok", pair.second());
    }

    @Test
    void mapHonorsConcurrencyCap() {
        AtomicInteger live = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        List<Integer> out = ReadJobs.map(List.of(1, 2, 3, 4), 2, n -> {
            int now = live.incrementAndGet();
            peak.accumulateAndGet(now, Math::max);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            live.decrementAndGet();
            return n * 10;
        });
        assertEquals(List.of(10, 20, 30, 40), out);
        assertTrue(peak.get() <= 2, "peak=" + peak.get());
    }
}
