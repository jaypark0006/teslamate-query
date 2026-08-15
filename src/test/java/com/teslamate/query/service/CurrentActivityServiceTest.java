package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.dao.SettingsDao;
import com.teslamate.query.dao.UpdateDao;
import com.teslamate.query.dto.ActivityStatus;
import com.teslamate.query.dto.ChargeType;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.SettingsEntity;
import com.teslamate.query.entity.UpdateEntity;
import com.teslamate.query.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentActivityServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T13:43:00Z");

    @Mock private CarDao carDao;
    @Mock private DriveDao driveDao;
    @Mock private ChargingProcessDao chargingProcessDao;
    @Mock private ChargeDao chargeDao;
    @Mock private PositionDao positionDao;
    @Mock private UpdateDao updateDao;
    @Mock private SettingsDao settingsDao;

    private CurrentActivityService service;

    @BeforeEach
    void setUp() {
        service = new CurrentActivityService(
                carDao, driveDao, chargingProcessDao, chargeDao, positionDao, updateDao, settingsDao,
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(carDao.findById(1L)).thenReturn(Optional.of(car()));
    }

    @Test
    void statusIsParkingFromLastDriveEnd() {
        Instant driveEnd = Instant.parse("2026-08-13T07:43:16Z");
        Instant posTime = Instant.parse("2026-08-13T07:46:43Z");
        when(chargingProcessDao.findIds(argThat(CurrentActivityServiceTest::openEnded), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(driveDao.findIds(argThat(CurrentActivityServiceTest::openEnded), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(driveDao.findIds(argThat(CurrentActivityServiceTest::completed), anyInt(), anyInt()))
                .thenReturn(List.of(5188L));
        when(driveDao.findById(5188L)).thenReturn(Optional.of(drive(5188L,
                Instant.parse("2026-08-13T07:38:21Z"), driveEnd, 54138.01, null)));
        when(chargingProcessDao.findIds(argThat(CurrentActivityServiceTest::completed), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(positionDao.findLatestByCarId(1L)).thenReturn(Optional.of(position(posTime, 39, "161.58", 54139.1)));
        when(settingsDao.find()).thenReturn(Optional.of(settings("rated")));
        when(updateDao.findIds(ArgumentMatchers.any(), anyInt(), anyInt())).thenReturn(List.of(17L));
        when(updateDao.findById(17L)).thenReturn(Optional.of(
                new UpdateEntity(17L, 1L, Instant.parse("2026-08-04T02:30:31Z"), Instant.parse("2026-08-04T02:33:39Z"), "2026.8.3.6")));

        var dto = service.status(1L);
        assertEquals(ActivityStatus.PARKING, dto.status());
        assertEquals("Y", dto.model());
        assertEquals("Apollo19MetallicShadow", dto.wheelType());
        assertEquals(39, dto.batteryLevel());
        assertEquals(new BigDecimal("161.58"), dto.rangeKm());
        assertEquals(54139.1, dto.odometerKm());
        assertEquals("2026.8.3.6", dto.firmwareVersion());
        assertEquals(ActivityClassifier.minutesBetween(driveEnd, NOW), dto.statusDurationMin());
    }

    @Test
    void chargingEndpoint404WhenParked() {
        idleLookups();
        assertThrows(NotFoundException.class, () -> service.charging(1L));
    }

    @Test
    void chargingReturnsOpenSession() {
        when(chargingProcessDao.findIds(argThat(CurrentActivityServiceTest::openEnded), anyInt(), anyInt()))
                .thenReturn(List.of(365L));
        when(chargingProcessDao.findById(365L)).thenReturn(Optional.of(chargeProcess()));
        when(driveDao.findIds(ArgumentMatchers.any(), anyInt(), anyInt())).thenReturn(List.of());
        when(positionDao.findLatestByCarId(1L)).thenReturn(Optional.empty());
        when(settingsDao.find()).thenReturn(Optional.of(settings("rated")));
        when(chargeDao.findLatestByProcessIds(List.of(365L))).thenReturn(Optional.of(chargeSample()));

        var dto = service.charging(1L);
        assertEquals(365L, dto.chargingProcessId());
        assertEquals(new BigDecimal("31.72"), dto.energyAddedKwh());
        assertEquals(44, dto.startBatteryLevel());
        assertEquals(new BigDecimal("185.46"), dto.startRangeKm());
        assertEquals(ChargeType.DC, dto.chargeType());
    }

    @Test
    void unknownCar404() {
        when(carDao.findById(9L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.status(9L));
    }

    @Test
    void pickRangePrefersSettings() {
        assertEquals(new BigDecimal("10"), CurrentActivityService.pickRange(
                new BigDecimal("10"), new BigDecimal("9"), "ideal"));
        assertEquals(new BigDecimal("9"), CurrentActivityService.pickRange(
                new BigDecimal("10"), new BigDecimal("9"), "rated"));
    }

    private void idleLookups() {
        when(chargingProcessDao.findIds(argThat(CurrentActivityServiceTest::openEnded), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(driveDao.findIds(argThat(CurrentActivityServiceTest::openEnded), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(driveDao.findIds(argThat(CurrentActivityServiceTest::completed), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(chargingProcessDao.findIds(argThat(CurrentActivityServiceTest::completed), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(positionDao.findLatestByCarId(1L)).thenReturn(Optional.empty());
    }

    private static boolean openEnded(com.teslamate.query.db.JdbiCondition c) {
        return c != null && c.whereClause().contains("end_date IS NULL")
                && !c.whereClause().contains("end_date IS NOT NULL");
    }

    private static boolean completed(com.teslamate.query.db.JdbiCondition c) {
        return c != null && c.whereClause().contains("end_date IS NOT NULL");
    }

    private static CarEntity car() {
        return new CarEntity(1L, 1L, 1L, "VIN", null, "Y", 0.13, "50", "SR",
                "StealthGrey", "Apollo19MetallicShadow", null, 1, 1L);
    }

    private static SettingsEntity settings(String range) {
        return new SettingsEntity(1L, "km", "C", "bar", range, null, null, "zh");
    }

    private static DriveEntity drive(Long id, Instant start, Instant end, Double startKm, Double distance) {
        return new DriveEntity(
                id, 1L, start, end,
                null, null, null, null, null,
                new BigDecimal("163.75"), new BigDecimal("161.72"),
                new BigDecimal("163.75"), new BigDecimal("161.72"),
                startKm, null, distance, null, null, null,
                null, null, null, null, null, null);
    }

    private static ChargingProcessEntity chargeProcess() {
        return new ChargingProcessEntity(
                365L, 1L,
                Instant.parse("2026-08-01T16:05:46Z"), null,
                new BigDecimal("31.72"), new BigDecimal("33.26"),
                new BigDecimal("185.46"), null,
                new BigDecimal("185.46"), null,
                44, null, null, null, null, null, null, null);
    }

    private static ChargeEntity chargeSample() {
        return new ChargeEntity(
                1L, 365L, Instant.parse("2026-08-01T16:20:00Z"),
                60, 60, new BigDecimal("10"),
                80, 400, 200, 3,
                true, "Gb", "GB_DC",
                new BigDecimal("200"), new BigDecimal("200"),
                new BigDecimal("28"), false);
    }

    private static PositionEntity position(Instant date, int battery, String range, double odo) {
        return new PositionEntity(
                10L, 1L, null, date,
                new BigDecimal("29.518242"), new BigDecimal("106.460280"),
                null, null, 0, odo,
                new BigDecimal(range), new BigDecimal("209.26"), new BigDecimal(range),
                battery, battery,
                new BigDecimal("33.5"), new BigDecimal("31.0"),
                0, new BigDecimal("21.0"), new BigDecimal("21.0"),
                false, false, false, false, false,
                new BigDecimal("3.0"), new BigDecimal("3.0"), new BigDecimal("3.0"), new BigDecimal("3.0"));
    }
}
