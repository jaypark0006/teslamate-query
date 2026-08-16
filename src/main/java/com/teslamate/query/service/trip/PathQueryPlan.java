package com.teslamate.query.service.trip;

import com.teslamate.query.entity.DriveEntity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pick a positions SQL strategy from the actual drive list, not the calendar
 * window. Short hops stay on start/end; longer drives share an 8k-point budget.
 */
public final class PathQueryPlan {

    static final int BUDGET = PathSimplify.MAX_WINDOW_POINTS;
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
        int minBucket = Integer.MAX_VALUE;
        boolean single = need.size() == 1;
        for (int i = 0; i < need.size(); i++) {
            DriveEntity d = need.get(i);
            int sec = durationSec(d);
            int target = targetPoints(need.size(), weights[i], weightSum);
            int bucket = single ? 2 : quantize(Math.max(1, sec / Math.max(target, 1)), false);
            minBucket = Math.min(minBucket, bucket);
            queries.add(new Query(d.id(), bucket, target));
        }
        return new PathQueryPlan(queries, skipped, epsilonForBucket(minBucket));
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

    static int quantize(int seconds, boolean singleDrive) {
        int min = singleDrive ? 2 : 5;
        int v = Math.max(seconds, min);
        for (int b : BUCKETS) {
            if (b >= v) {
                return b;
            }
        }
        return BUCKETS[BUCKETS.length - 1];
    }

    static double epsilonForBucket(int bucketSec) {
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
