package com.teslamate.query.service.trip;

import com.teslamate.query.dto.TimelineKind;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a DRIVE / CHARGE / PARK sequence for a time window.
 * Charges occupy time (they are not parks). Gaps shorter than {@code minParkMin} are omitted.
 */
public final class ActivityTimelineComposer {

    private ActivityTimelineComposer() {}

    public static List<ActivitySpan> compose(
            List<DriveEntity> drives,
            List<ChargingProcessEntity> charges,
            Instant windowFrom,
            Instant windowTo,
            Instant now,
            int minParkMin,
            Long seedPositionId,
            Long seedDriveId
    ) {
        if (windowFrom == null || windowTo == null || !windowTo.isAfter(windowFrom)) {
            return List.of();
        }
        Instant clock = now == null ? Instant.now() : now;
        int minPark = Math.max(minParkMin, 0);

        List<Busy> busy = new ArrayList<>();
        if (drives != null) {
            for (DriveEntity d : drives) {
                if (d == null || d.startDate() == null) {
                    continue;
                }
                Instant end = d.endDate() == null ? clock : d.endDate();
                Instant start = clipStart(d.startDate(), windowFrom);
                Instant stop = clipEnd(end, windowTo);
                if (!stop.isAfter(start)) {
                    continue;
                }
                Long loc = d.endPositionId() != null ? d.endPositionId() : d.startPositionId();
                busy.add(new Busy(TimelineKind.DRIVE, d.id(), start, stop, loc, d.id()));
            }
        }
        if (charges != null) {
            for (ChargingProcessEntity c : charges) {
                if (c == null || c.startDate() == null) {
                    continue;
                }
                Instant end = c.endDate() == null ? clock : c.endDate();
                Instant start = clipStart(c.startDate(), windowFrom);
                Instant stop = clipEnd(end, windowTo);
                if (!stop.isAfter(start)) {
                    continue;
                }
                busy.add(new Busy(TimelineKind.CHARGE, c.id(), start, stop, c.positionId(), seedDriveId));
            }
        }
        busy.sort(Comparator
                .comparing(Busy::start)
                .thenComparing(b -> b.kind == TimelineKind.DRIVE ? 0 : 1)
                .thenComparing(b -> b.sourceId == null ? 0L : b.sourceId));

        if (busy.isEmpty() && seedDriveId == null) {
            return List.of();
        }

        List<ActivitySpan> out = new ArrayList<>();
        Instant cursor = windowFrom;
        Long lastPos = seedPositionId;
        Long lastDrive = seedDriveId;
        for (Busy b : busy) {
            if (b.start.isAfter(cursor)) {
                addPark(out, cursor, b.start, minPark, lastPos, lastDrive);
            }
            out.add(new ActivitySpan(b.kind, b.sourceId, b.start, b.end, minutes(b.start, b.end),
                    b.locationPositionId, b.kind == TimelineKind.DRIVE ? b.sourceId : lastDrive));
            if (b.end.isAfter(cursor)) {
                cursor = b.end;
            }
            if (b.locationPositionId != null) {
                lastPos = b.locationPositionId;
            }
            if (b.kind == TimelineKind.DRIVE && b.sourceId != null) {
                lastDrive = b.sourceId;
            }
        }
        Instant trailEnd = windowTo.isAfter(clock) ? clock : windowTo;
        if (trailEnd.isAfter(cursor)) {
            addPark(out, cursor, trailEnd, minPark, lastPos, lastDrive);
        }
        return List.copyOf(out);
    }

    private static void addPark(List<ActivitySpan> out, Instant start, Instant end,
                                int minPark, Long positionId, Long afterDriveId) {
        double mins = minutes(start, end);
        if (mins < minPark) {
            return;
        }
        out.add(new ActivitySpan(TimelineKind.PARK, null, start, end, round1(mins), positionId, afterDriveId));
    }

    private static Instant clipStart(Instant start, Instant from) {
        return start.isBefore(from) ? from : start;
    }

    private static Instant clipEnd(Instant end, Instant to) {
        return end.isAfter(to) ? to : end;
    }

    private static double minutes(Instant start, Instant end) {
        return Duration.between(start, end).toMillis() / 60_000.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private record Busy(
            TimelineKind kind,
            Long sourceId,
            Instant start,
            Instant end,
            Long locationPositionId,
            Long driveId
    ) {}
}
