package com.teslamate.query.service.trip;

import com.teslamate.query.entity.DriveEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Derives PARK gaps from completed/incomplete drives (no park table).
 * Short gaps merge drives into one cluster; parks are the gaps between clusters.
 */
public final class ParkComposer {

    private ParkComposer() {}

    public static List<ParkGap> compose(
            List<DriveEntity> drives,
            Instant windowFrom,
            Instant windowTo,
            Instant now,
            int microDriveThresholdMin,
            int minParkMin
    ) {
        if (drives == null || drives.isEmpty() || windowFrom == null || windowTo == null) {
            return List.of();
        }
        Instant clock = now == null ? Instant.now() : now;
        int micro = Math.max(microDriveThresholdMin, 0);
        int minPark = Math.max(minParkMin, 0);

        List<DriveEntity> ordered = drives.stream()
                .filter(d -> d.startDate() != null)
                .sorted(Comparator.comparing(DriveEntity::startDate))
                .toList();
        if (ordered.isEmpty()) {
            return List.of();
        }

        List<Cluster> clusters = cluster(ordered, clock, micro);
        List<ParkGap> out = new ArrayList<>();
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cur = clusters.get(i);
            if (cur.end == null) {
                continue;
            }
            Instant parkEnd = (i + 1 < clusters.size())
                    ? clusters.get(i + 1).start
                    : clock;
            if (parkEnd == null || !parkEnd.isAfter(cur.end)) {
                continue;
            }
            Instant clippedStart = cur.end.isAfter(windowFrom) ? cur.end : windowFrom;
            Instant clippedEnd = parkEnd.isBefore(windowTo) ? parkEnd : windowTo;
            if (!clippedEnd.isAfter(clippedStart)) {
                continue;
            }
            double mins = Duration.between(clippedStart, clippedEnd).toMillis() / 60_000.0;
            if (mins < minPark) {
                continue;
            }
            out.add(new ParkGap(clippedStart, clippedEnd, round1(mins), cur.endPositionId, cur.lastDriveId));
        }
        return out;
    }

    private static List<Cluster> cluster(List<DriveEntity> ordered, Instant now, int microMin) {
        List<Cluster> clusters = new ArrayList<>();
        Cluster current = null;
        Instant prevEnd = null;
        for (DriveEntity d : ordered) {
            Instant start = d.startDate();
            Instant end = d.endDate() == null ? now : d.endDate();
            boolean newCluster = prevEnd == null
                    || Duration.between(prevEnd, start).toMinutes() >= microMin;
            if (newCluster || current == null) {
                current = new Cluster();
                current.start = start;
                clusters.add(current);
            }
            current.end = end;
            current.lastDriveId = d.id();
            current.endPositionId = d.endPositionId();
            prevEnd = end;
        }
        return clusters;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static final class Cluster {
        Instant start;
        Instant end;
        Long lastDriveId;
        Long endPositionId;
    }
}
