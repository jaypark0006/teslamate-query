package com.teslamate.query.service.trip;

import com.teslamate.query.entity.PositionPathPoint;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Douglas–Peucker: drop points that sit close to the chord, keep corners.
 * Operates in local meters so epsilon is independent of longitude.
 */
public final class PathSimplify {

    static final double DEFAULT_EPSILON_M = 12.0;
    /** Safety cap for a whole map window after per-drive simplify. */
    static final int MAX_WINDOW_POINTS = 16_000;

    private PathSimplify() {}

    /**
     * Time-range LOD. Grafana Geomap cannot send map zoom; the picker window
     * is the zoom. Wide windows keep shape, not 1 Hz.
     */
    public static double epsilonMeters(long windowSec) {
        long days = Math.max(windowSec, 0) / 86_400L;
        if (days <= 2) {
            return 8.0;
        }
        if (days <= 7) {
            return 25.0;
        }
        if (days <= 30) {
            return 80.0;
        }
        if (days <= 90) {
            return 250.0;
        }
        return 500.0;
    }

    /** 0 = every GPS point. Wider windows sample in SQL before DP. */
    public static int sampleBucketSeconds(long windowSec) {
        long days = Math.max(windowSec, 0) / 86_400L;
        if (days <= 2) {
            return 0;
        }
        if (days <= 7) {
            return 5;
        }
        if (days <= 30) {
            return 15;
        }
        if (days <= 90) {
            return 60;
        }
        return 180;
    }

    public static int maxPointsPerDrive(long windowSec, int driveCount) {
        int budget = driveCount <= 0 ? MAX_WINDOW_POINTS : Math.max(6, MAX_WINDOW_POINTS / driveCount);
        long days = Math.max(windowSec, 0) / 86_400L;
        int soft = days <= 2 ? 400 : days <= 7 ? 80 : days <= 30 ? 40 : days <= 90 ? 16 : 8;
        return Math.min(soft, budget);
    }

    /** Keep first/last and a uniform sample so a long polyline stays under {@code max}. */
    public static List<PositionPathPoint> cap(List<PositionPathPoint> points, int max) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        if (max <= 2 || points.size() <= max) {
            return points.size() <= 2 ? List.copyOf(points) : points;
        }
        List<PositionPathPoint> out = new ArrayList<>(max);
        int last = points.size() - 1;
        out.add(points.getFirst());
        for (int i = 1; i < max - 1; i++) {
            int idx = (int) Math.round(i * (double) last / (max - 1));
            PositionPathPoint p = points.get(idx);
            if (!p.equals(out.getLast())) {
                out.add(p);
            }
        }
        if (!points.get(last).equals(out.getLast())) {
            out.add(points.get(last));
        }
        return out;
    }

    public static List<PositionPathPoint> douglasPeucker(List<PositionPathPoint> points) {
        return douglasPeucker(points, DEFAULT_EPSILON_M);
    }

    public static List<PositionPathPoint> douglasPeucker(List<PositionPathPoint> points, double epsilonMeters) {
        if (points == null || points.size() <= 2) {
            return points == null ? List.of() : List.copyOf(points);
        }
        double eps = Math.max(epsilonMeters, 0.5);
        int n = points.size();
        boolean[] keep = new boolean[n];
        keep[0] = true;
        keep[n - 1] = true;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, n - 1});
        while (!stack.isEmpty()) {
            int[] range = stack.pop();
            int lo = range[0];
            int hi = range[1];
            double max = -1;
            int idx = -1;
            PositionPathPoint a = points.get(lo);
            PositionPathPoint b = points.get(hi);
            for (int i = lo + 1; i < hi; i++) {
                double d = distPointToSegMeters(points.get(i), a, b);
                if (d > max) {
                    max = d;
                    idx = i;
                }
            }
            if (idx >= 0 && max > eps) {
                keep[idx] = true;
                stack.push(new int[]{lo, idx});
                stack.push(new int[]{idx, hi});
            }
        }
        List<PositionPathPoint> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                out.add(points.get(i));
            }
        }
        return out;
    }

    static double distPointToSegMeters(PositionPathPoint p, PositionPathPoint a, PositionPathPoint b) {
        return distPointToSegMeters(
                lon(p), lat(p), lon(a), lat(a), lon(b), lat(b));
    }

    static double distPointToSegMeters(
            double lon, double lat, double lon1, double lat1, double lon2, double lat2) {
        double mid = Math.toRadians((lat1 + lat2) * 0.5);
        double mx = 111_320.0 * Math.cos(mid);
        double my = 110_540.0;
        double ax = lon1 * mx;
        double ay = lat1 * my;
        double bx = lon2 * mx;
        double by = lat2 * my;
        double px = lon * mx;
        double py = lat * my;
        double dx = bx - ax;
        double dy = by - ay;
        double len2 = dx * dx + dy * dy;
        if (len2 < 1e-6) {
            return Math.hypot(px - ax, py - ay);
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / len2;
        t = Math.max(0.0, Math.min(1.0, t));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    private static double lon(PositionPathPoint p) {
        BigDecimal v = p.longitude();
        return v == null ? 0 : v.doubleValue();
    }

    private static double lat(PositionPathPoint p) {
        BigDecimal v = p.latitude();
        return v == null ? 0 : v.doubleValue();
    }
}
