package com.teslamate.query.service.trip;

import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Groups fragmented charging processes without mutating TeslaMate data. */
public final class ChargeSessionComposer {

    private ChargeSessionComposer() {}

    public static List<List<ChargingProcessEntity>> newestSessions(
            List<ChargingProcessEntity> processes,
            Map<Long, PositionEntity> positions,
            List<DriveEntity> drives,
            int mergeGapMin,
            int mergeDistanceM,
            int limit
    ) {
        if (processes == null || processes.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<ChargingProcessEntity> ordered = processes.stream()
                .filter(p -> p != null && p.startDate() != null)
                .sorted(Comparator.comparing(ChargingProcessEntity::startDate))
                .toList();
        if (ordered.isEmpty()) {
            return List.of();
        }

        int gapLimit = Math.max(mergeGapMin, 0);
        int distanceLimit = Math.max(mergeDistanceM, 0);
        Map<Long, PositionEntity> byPosition = positions == null ? Map.of() : positions;
        List<DriveEntity> knownDrives = drives == null ? List.of() : drives;
        List<List<ChargingProcessEntity>> groups = new ArrayList<>();
        List<ChargingProcessEntity> current = null;
        for (ChargingProcessEntity next : ordered) {
            if (current == null || !canMerge(
                    current.getLast(), next, byPosition, knownDrives, gapLimit, distanceLimit)) {
                current = new ArrayList<>();
                groups.add(current);
            }
            current.add(next);
        }
        Collections.reverse(groups);
        return groups.stream().limit(limit).map(List::copyOf).toList();
    }

    private static boolean canMerge(
            ChargingProcessEntity previous,
            ChargingProcessEntity next,
            Map<Long, PositionEntity> positions,
            List<DriveEntity> drives,
            int gapLimit,
            int distanceLimit
    ) {
        if (gapLimit <= 0 || previous.endDate() == null || next.startDate() == null) {
            return false;
        }
        long gap = Duration.between(previous.endDate(), next.startDate()).toMinutes();
        if (gap < 0 || gap > gapLimit || hasDriveBetween(previous, next, drives)) {
            return false;
        }
        return sameLocation(previous, next, positions, distanceLimit);
    }

    private static boolean sameLocation(
            ChargingProcessEntity a,
            ChargingProcessEntity b,
            Map<Long, PositionEntity> positions,
            int distanceLimit
    ) {
        if (a.geofenceId() != null && b.geofenceId() != null) {
            return a.geofenceId().equals(b.geofenceId());
        }
        if (a.addressId() != null && b.addressId() != null) {
            return a.addressId().equals(b.addressId());
        }
        if (a.positionId() != null && a.positionId().equals(b.positionId())) {
            return true;
        }
        PositionEntity pa = a.positionId() == null ? null : positions.get(a.positionId());
        PositionEntity pb = b.positionId() == null ? null : positions.get(b.positionId());
        if (distanceLimit <= 0 || !hasCoordinates(pa) || !hasCoordinates(pb)) {
            return false;
        }
        return haversineMeters(pa, pb) <= distanceLimit;
    }

    private static boolean hasDriveBetween(
            ChargingProcessEntity previous,
            ChargingProcessEntity next,
            List<DriveEntity> drives
    ) {
        return drives.stream()
                .filter(d -> d != null && d.startDate() != null)
                .anyMatch(d -> d.startDate().isBefore(next.startDate())
                        && (d.endDate() == null || d.endDate().isAfter(previous.endDate())));
    }

    private static boolean hasCoordinates(PositionEntity p) {
        return p != null && p.latitude() != null && p.longitude() != null;
    }

    private static double haversineMeters(PositionEntity a, PositionEntity b) {
        double lat1 = Math.toRadians(a.latitude().doubleValue());
        double lat2 = Math.toRadians(b.latitude().doubleValue());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude().doubleValue() - a.longitude().doubleValue());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 12_742_000.0 * Math.asin(Math.min(1, Math.sqrt(h)));
    }
}
