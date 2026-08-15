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

    private PathSimplify() {}

    public static double epsilonMeters(long windowSec) {
        if (windowSec <= 2 * 86_400L) {
            return 8.0;
        }
        if (windowSec <= 10 * 86_400L) {
            return 12.0;
        }
        return 20.0;
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
