package com.teslamate.query.service.trip;

import java.util.ArrayList;
import java.util.List;

/**
 * Evenly sample position primary keys between a drive's start and end id.
 * Callers then {@code WHERE id IN (...)} — index lookups, not a drive-id scan.
 */
public final class PathSampler {

    private PathSampler() {}

    public static List<Long> sampleIds(long startId, long endId, int maxPoints) {
        long a = Math.min(startId, endId);
        long b = Math.max(startId, endId);
        int max = Math.max(maxPoints, 2);
        if (a == b) {
            return List.of(a);
        }
        List<Long> out = new ArrayList<>(max);
        double step = (b - a) / (double) (max - 1);
        long last = -1;
        for (int i = 0; i < max; i++) {
            long id = a + Math.round(i * step);
            if (id != last) {
                out.add(id);
                last = id;
            }
        }
        if (last != b) {
            out.add(b);
        }
        return out;
    }
}
