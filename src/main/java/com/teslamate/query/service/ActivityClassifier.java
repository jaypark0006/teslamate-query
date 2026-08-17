package com.teslamate.query.service;

import com.teslamate.query.dto.ActivityStatus;
import com.teslamate.query.dto.ChargeType;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

/** Shared PARKING / DRIVING / CHARGING resolution (no TeslaMate {@code states} table). */
public final class ActivityClassifier {

    private ActivityClassifier() {}

    public static ActivityStatus status(
            Optional<ChargingProcessEntity> openCharge,
            Optional<DriveEntity> openDrive
    ) {
        if (openCharge != null && openCharge.isPresent()) {
            return ActivityStatus.CHARGING;
        }
        if (openDrive != null && openDrive.isPresent()) {
            return ActivityStatus.DRIVING;
        }
        return ActivityStatus.PARKING;
    }

    public static Instant statusSince(
            ActivityStatus status,
            Optional<ChargingProcessEntity> openCharge,
            Optional<DriveEntity> openDrive,
            Optional<DriveEntity> lastCompletedDrive,
            Optional<ChargingProcessEntity> lastCompletedCharge,
            Instant fallback
    ) {
        return switch (status) {
            case CHARGING -> openCharge.map(e->e.startDate().toInstant(ZoneOffset.UTC)).orElse(fallback);
            case DRIVING -> openDrive.map(e->e.startDate().toInstant(ZoneOffset.UTC)).orElse(fallback);
            case PARKING -> parkingSince(lastCompletedDrive, lastCompletedCharge, fallback);
        };
    }

    public static Instant parkingSince(
            Optional<DriveEntity> lastCompletedDrive,
            Optional<ChargingProcessEntity> lastCompletedCharge,
            Instant fallback
    ) {
        Instant driveEnd = lastCompletedDrive.map(e->e.endDate().toInstant(ZoneOffset.UTC)).orElse(null);
        Instant chargeEnd = lastCompletedCharge.map(e->e.endDate().toInstant(ZoneOffset.UTC)).orElse(null);
        if (driveEnd == null) {
            return chargeEnd != null ? chargeEnd : fallback;
        }
        if (chargeEnd == null) {
            return driveEnd;
        }
        return chargeEnd.isAfter(driveEnd) ? chargeEnd : driveEnd;
    }

    public static long minutesBetween(Instant from, Instant to) {
        if (from == null || to == null || to.isBefore(from)) {
            return 0L;
        }
        return Duration.between(from, to).toMinutes();
    }

    public static ChargeType chargeType(Boolean fastChargerPresent, String fastChargerType, String cable) {
        if (Boolean.TRUE.equals(fastChargerPresent) || looksDc(fastChargerType) || looksDc(cable)) {
            return ChargeType.DC;
        }
        if (fastChargerPresent == null && isBlank(fastChargerType) && isBlank(cable)) {
            return null;
        }
        return ChargeType.AC;
    }

    private static boolean looksDc(String raw) {
        if (isBlank(raw)) {
            return false;
        }
        String v = raw.toLowerCase(Locale.ROOT);
        return v.contains("dc")
                || v.contains("combo")
                || v.contains("chademo")
                || "tesla".equals(v)
                || "gb".equals(v)
                || "mc".equals(v);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
