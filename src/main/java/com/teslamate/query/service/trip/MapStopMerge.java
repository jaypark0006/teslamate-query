package com.teslamate.query.service.trip;

import com.teslamate.query.dto.MapPointDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collapse park/charge markers that sit on the same spot (home, work, a charger)
 * so Grafana does not stack circles and labels.
 */
public final class MapStopMerge {

    /** ~110 m — same driveway / charger. */
    static final double CELL_DEG = 0.001;

    private MapStopMerge() {}

    public static List<MapPointDto> mergeStops(List<MapPointDto> points) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        List<MapPointDto> drives = new ArrayList<>();
        Map<String, List<MapPointDto>> groups = new LinkedHashMap<>();
        for (MapPointDto p : points) {
            if (p == null) {
                continue;
            }
            if ("drive".equals(p.kind())) {
                drives.add(p);
                continue;
            }
            if (p.latitude() == null || p.longitude() == null) {
                continue;
            }
            groups.computeIfAbsent(key(p), k -> new ArrayList<>()).add(p);
        }
        List<MapPointDto> out = new ArrayList<>(drives.size() + groups.size());
        out.addAll(drives);
        for (List<MapPointDto> group : groups.values()) {
            out.add(collapse(group));
        }
        return out;
    }

    public static String parkLabel(int count, Double totalMin, boolean ongoing) {
        String dur = ongoing ? "-" : formatDuration(totalMin);
        if (count <= 1) {
            return dur == null ? "P" : "P " + dur;
        }
        return dur == null ? "P ×" + count : "P ×" + count + " · " + dur;
    }

    public static String chargeLabel(int count, String type, Double energyKwh, Double totalMin) {
        String prefix = (type == null || type.isBlank()) ? "Charge" : type;
        if (count > 1) {
            prefix = prefix + " ×" + count;
        }
        String energy = energyKwh == null ? null : "+" + trim(energyKwh) + " kWh";
        String dur = formatDuration(totalMin);
        if (energy == null && dur == null) {
            return prefix;
        }
        if (energy == null) {
            return prefix + " · " + dur;
        }
        if (dur == null) {
            return prefix + " " + energy;
        }
        return prefix + " " + energy + " · " + dur;
    }

    private static MapPointDto collapse(List<MapPointDto> group) {
        MapPointDto first = group.getFirst();
        if (group.size() == 1) {
            return withLabel(first, labelFor(first, 1, first.durationMin(), first.energyKwh(), isOngoing(first)));
        }
        double lat = 0;
        double lon = 0;
        double dur = 0;
        double energy = 0;
        boolean anyEnergy = false;
        boolean anyDur = false;
        boolean ongoing = false;
        MapPointDto latest = first;
        for (MapPointDto p : group) {
            lat += p.latitude();
            lon += p.longitude();
            if (p.durationMin() != null) {
                dur += p.durationMin();
                anyDur = true;
            }
            if (isOngoing(p)) {
                ongoing = true;
            }
            if (p.energyKwh() != null) {
                energy += p.energyKwh();
                anyEnergy = true;
            }
            if (latest.time() == null || (p.time() != null && p.time().isAfter(latest.time()))) {
                latest = p;
            }
        }
        int n = group.size();
        String type = latest.label() != null && (latest.label().startsWith("DC") || "DC".equalsIgnoreCase(latest.label()))
                ? "DC"
                : chargeTypeFrom(latest);
        String text = labelFor(latest, n, anyDur ? dur : null, anyEnergy ? energy : null, ongoing);
        return new MapPointDto(
                latest.time(),
                lat / n,
                lon / n,
                latest.kind(),
                latest.id(),
                latest.seq(),
                null,
                null,
                latest.color(),
                latest.label(),
                text,
                anyDur ? dur : null,
                anyEnergy ? energy : null);
    }

    private static String chargeTypeFrom(MapPointDto p) {
        String label = p.label();
        if (label != null && (label.startsWith("DC") || label.contains("DC"))) {
            return "DC";
        }
        if (label != null && (label.startsWith("AC") || label.contains("AC"))) {
            return "AC";
        }
        return p.kind() != null && p.kind().equals("charge") ? "AC" : null;
    }

    private static String labelFor(MapPointDto sample, int count, Double dur, Double energy, boolean ongoing) {
        if ("park".equals(sample.kind())) {
            return parkLabel(count, dur, ongoing);
        }
        return chargeLabel(count, chargeTypeFrom(sample), energy, dur);
    }

    private static MapPointDto withLabel(MapPointDto p, String text) {
        return new MapPointDto(
                p.time(), p.latitude(), p.longitude(), p.kind(), p.id(), p.seq(),
                p.heading(), p.elapsedMin(), p.color(), p.label(), text,
                p.durationMin(), p.energyKwh());
    }

    private static boolean isOngoing(MapPointDto p) {
        return "park".equals(p.kind()) && p.durationMin() == null;
    }

    private static String key(MapPointDto p) {
        long la = (long) Math.floor(p.latitude() / CELL_DEG);
        long lo = (long) Math.floor(p.longitude() / CELL_DEG);
        return p.kind() + ":" + la + ":" + lo;
    }

    private static String formatDuration(Double minutes) {
        if (minutes == null) {
            return null;
        }
        long m = Math.round(minutes);
        if (m < 60) {
            return m + " min";
        }
        long h = m / 60;
        long rem = m % 60;
        return rem == 0 ? h + "h" : h + "h " + rem + "m";
    }

    private static String trim(double v) {
        if (Math.abs(v - Math.round(v)) < 0.05) {
            return String.valueOf(Math.round(v));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
