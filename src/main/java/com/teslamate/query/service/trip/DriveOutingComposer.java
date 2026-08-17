package com.teslamate.query.service.trip;

import com.teslamate.query.entity.DriveEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Groups completed drives whose park gap is shorter than {@code mergeGapMin}.
 * {@code mergeGapMin <= 0} leaves every drive as its own outing.
 */
public final class DriveOutingComposer {

    private DriveOutingComposer() {}

    public static List<List<DriveEntity>> newestOutings(List<DriveEntity> drives, int mergeGapMin, int limit) {
        if (drives == null || drives.isEmpty() || limit < 1) {
            return List.of();
        }
        List<DriveEntity> oldestFirst = drives.stream()
                .filter(d -> d.startDate() != null)
                .sorted(Comparator.comparing(DriveEntity::startDate))
                .toList();
        List<List<DriveEntity>> clusters = new ArrayList<>();
        if (mergeGapMin <= 0) {
            for (DriveEntity d : oldestFirst) {
                clusters.add(List.of(d));
            }
        } else {
            List<DriveEntity> current = new ArrayList<>();
            Instant prevEnd = null;
            for (DriveEntity d : oldestFirst) {
                Instant end = d.endDate() != null ? d.endDate().toInstant(ZoneOffset.UTC) : d.startDate().toInstant(ZoneOffset.UTC);
                boolean newOuting = prevEnd == null
                        || Duration.between(prevEnd, d.startDate().toInstant(ZoneOffset.UTC)).toMinutes() >= mergeGapMin;
                if (newOuting && !current.isEmpty()) {
                    clusters.add(List.copyOf(current));
                    current = new ArrayList<>();
                }
                current.add(d);
                prevEnd = end;
            }
            if (!current.isEmpty()) {
                clusters.add(List.copyOf(current));
            }
        }
        int from = Math.max(0, clusters.size() - limit);
        List<List<DriveEntity>> newest = new ArrayList<>(clusters.subList(from, clusters.size()));
        newest.sort((a, b) -> b.getFirst().startDate().compareTo(a.getFirst().startDate()));
        return newest;
    }
}
