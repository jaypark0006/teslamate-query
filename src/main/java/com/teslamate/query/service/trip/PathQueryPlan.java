package com.teslamate.query.service.trip;

import com.teslamate.query.entity.DriveEntity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pick a positions SQL strategy from the actual drive list, not the calendar
 * window. Micro hops stay on start/end. A highlight or a short window is
 * cheaper as raw 1 Hz than as a SQL time-bucket + Douglas–Peucker pass.
 */
public final class PathQueryPlan {

    static final int BUDGET = PathSimplify.MAX_WINDOW_POINTS;
    /** ~15 min at 1 Hz. Window-function + DP costs more than reading these rows. */
    static final int SHORT_FULL_SEC = 15 * 60;
    /** Typical one-day outing. Short drives in this set stay 1 Hz. */
    static final int DAY_SCALE_DRIVES = 12;
    private static final int[] BUCKETS = {2, 5, 10, 15, 30, 60, 120, 180, 300};

    public record Query(long driveId, int bucketSec, int cap) {}

    public record Batch(int bucketSec, List<Long> driveIds, Map<Long, Integer> capById) {}

    private final List<Query> queries;
    private final int skipped;
    private final double epsilonM;

    private PathQueryPlan(List<Query> queries, int skipped, double epsilonM) {
        this.queries = List.copyOf(queries);
        this.skipped = skipped;
        this.epsilonM = epsilonM;
    }

    public List<Query> queries() {
        return queries;
    }

    public int skipped() {
        return skipped;
    }

    public double epsilonM() {
        return epsilonM;
    }

    public static PathQueryPlan of(List<DriveEntity> drives) {
        if (drives == null || drives.isEmpty()) {
            return new PathQueryPlan(List.of(), 0, 8.0);
        }
        List<DriveEntity> need = new ArrayList<>();
        int skipped = 0;
        long weightSum = 0;
        int[] weights = new int[drives.size()];
        int w = 0;
        for (DriveEntity d : drives) {
            if (d == null || d.id() == null || skipPath(d)) {
                skipped++;
                continue;
            }
            int weight = Math.max(1, durationSec(d));
            weights[w++] = weight;
            weightSum += weight;
            need.add(d);
        }
        if (need.isEmpty()) {
            return new PathQueryPlan(List.of(), skipped, 8.0);
        }
        List<Query> queries = new ArrayList<>(need.size());
        boolean windowFits = need.size() == 1 || weightSum <= BUDGET;
        boolean dayScale = need.size() <= DAY_SCALE_DRIVES;
        int minLodBucket = Integer.MAX_VALUE;
        for (int i = 0; i < need.size(); i++) {
            DriveEntity d = need.get(i);
            int sec = durationSec(d);
            boolean full = windowFits || (dayScale && sec <= SHORT_FULL_SEC);
            if (full) {
                queries.add(fullQuery(d.id(), sec));
            } else {
                int target = targetPoints(need.size(), weights[i], weightSum);
                int bucket = quantize(Math.max(1, sec / Math.max(target, 1)), 5);
                minLodBucket = Math.min(minLodBucket, bucket);
                queries.add(new Query(d.id(), bucket, target));
            }
        }
        double eps = minLodBucket == Integer.MAX_VALUE ? 0 : epsilonForBucket(minLodBucket);
        return new PathQueryPlan(queries, skipped, eps);
    }

    /**
     * Raw GPS when the trip is short enough that a 1 Hz guess fits the budget.
     * Cap is the per-drive safety net, not duration-seconds: TeslaMate is often 3–5 Hz.
     */
    static Query fullQuery(long driveId, int sec) {
        if (sec <= BUDGET) {
            return new Query(driveId, 0, BUDGET);
        }
        int bucket = quantize(Math.max(1, (sec + BUDGET - 1) / BUDGET), 2);
        return new Query(driveId, bucket, BUDGET);
    }

    public List<Batch> batches() {
        Map<Integer, List<Query>> byBucket = new LinkedHashMap<>();
        for (Query q : queries) {
            byBucket.computeIfAbsent(q.bucketSec(), k -> new ArrayList<>()).add(q);
        }
        List<Batch> out = new ArrayList<>();
        int n = PathSimplify.PATH_BATCH;
        for (Map.Entry<Integer, List<Query>> e : byBucket.entrySet()) {
            List<Query> group = e.getValue();
            for (int i = 0; i < group.size(); i += n) {
                List<Query> chunk = group.subList(i, Math.min(i + n, group.size()));
                List<Long> ids = new ArrayList<>(chunk.size());
                Map<Long, Integer> caps = new LinkedHashMap<>();
                for (Query q : chunk) {
                    ids.add(q.driveId());
                    caps.put(q.driveId(), q.cap());
                }
                out.add(new Batch(e.getKey(), ids, caps));
            }
        }
        return out;
    }

    static boolean skipPath(DriveEntity d) {
        int sec = durationSec(d);
        double km = d.distance() == null ? -1 : d.distance();
        if (sec < 150) {
            return true;
        }
        return km >= 0 && km < 0.35;
    }

    static int durationSec(DriveEntity d) {
        if (d.durationMin() != null && d.durationMin() > 0) {
            return d.durationMin() * 60;
        }
        if (d.startDate() != null && d.endDate() != null && d.endDate().isAfter(d.startDate())) {
            long s = Duration.between(d.startDate(), d.endDate()).getSeconds();
            return (int) Math.max(1, Math.min(s, Integer.MAX_VALUE));
        }
        return 60;
    }

    static int targetPoints(int driveCount, int weight, long weightSum) {
        if (driveCount <= 1) {
            return 400;
        }
        if (weightSum <= 0) {
            return Math.max(6, BUDGET / driveCount);
        }
        int share = (int) Math.max(6, Math.min(400, BUDGET * (long) weight / weightSum));
        int even = Math.max(6, BUDGET / driveCount);
        return Math.min(share, Math.max(even, 8));
    }

    static int quantize(int seconds, int minBucket) {
        int v = Math.max(seconds, minBucket);
        for (int b : BUCKETS) {
            if (b >= v) {
                return b;
            }
        }
        return BUCKETS[BUCKETS.length - 1];
    }

    public static double epsilonForBucket(int bucketSec) {
        if (bucketSec <= 2) {
            return 8.0;
        }
        if (bucketSec <= 10) {
            return 25.0;
        }
        if (bucketSec <= 30) {
            return 80.0;
        }
        return 250.0;
    }
}
