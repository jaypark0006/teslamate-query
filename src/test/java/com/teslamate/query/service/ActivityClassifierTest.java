package com.teslamate.query.service;

import com.teslamate.query.dto.ActivityStatus;
import com.teslamate.query.dto.ChargeType;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActivityClassifierTest {

    @Test
    void chargingWinsOverDrive() {
        assertEquals(ActivityStatus.CHARGING, ActivityClassifier.status(
                Optional.of(charge(null, Instant.parse("2026-08-01T00:00:00Z"), null)),
                Optional.of(drive(null, Instant.parse("2026-08-01T00:00:00Z"), null))));
        assertEquals(ActivityStatus.DRIVING, ActivityClassifier.status(
                Optional.empty(),
                Optional.of(drive(null, Instant.parse("2026-08-01T00:00:00Z"), null))));
        assertEquals(ActivityStatus.PARKING, ActivityClassifier.status(Optional.empty(), Optional.empty()));
    }

    @Test
    void parkingSinceIsLaterOfDriveOrChargeEnd() {
        Instant driveEnd = Instant.parse("2026-08-13T07:43:16Z");
        Instant chargeEnd = Instant.parse("2026-08-01T16:44:20Z");
        Instant fallback = Instant.parse("2026-08-13T07:46:43Z");
        assertEquals(driveEnd, ActivityClassifier.parkingSince(
                Optional.of(drive(1L, Instant.parse("2026-08-13T07:38:21Z"), driveEnd)),
                Optional.of(charge(1L, Instant.parse("2026-08-01T16:05:46Z"), chargeEnd)),
                fallback));
        assertEquals(chargeEnd, ActivityClassifier.parkingSince(
                Optional.empty(),
                Optional.of(charge(1L, Instant.parse("2026-08-01T16:05:46Z"), chargeEnd)),
                fallback));
        assertEquals(fallback, ActivityClassifier.parkingSince(Optional.empty(), Optional.empty(), fallback));
    }

    @Test
    void minutesClampWhenInverted() {
        Instant a = Instant.parse("2026-08-13T07:48:59Z");
        Instant b = Instant.parse("2026-08-15T13:43:00Z");
        assertEquals(java.time.Duration.between(a, b).toMinutes(), ActivityClassifier.minutesBetween(a, b));
        assertEquals(0L, ActivityClassifier.minutesBetween(b, a));
        assertEquals(0L, ActivityClassifier.minutesBetween(null, b));
    }

    @Test
    void chargeTypeFromSample() {
        assertEquals(ChargeType.DC, ActivityClassifier.chargeType(true, "Gb", "GB_DC"));
        assertEquals(ChargeType.DC, ActivityClassifier.chargeType(false, null, "GB_DC"));
        assertEquals(ChargeType.AC, ActivityClassifier.chargeType(false, null, "GB_AC"));
        assertNull(ActivityClassifier.chargeType(null, null, null));
    }

    private static DriveEntity drive(Long id, Instant start, Instant end) {
        return new DriveEntity(
                id, 1L, start, end,
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    private static ChargingProcessEntity charge(Long id, Instant start, Instant end) {
        return new ChargingProcessEntity(
                id, 1L, start, end,
                null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null);
    }
}
