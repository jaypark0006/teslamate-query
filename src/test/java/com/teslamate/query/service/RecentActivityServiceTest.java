package com.teslamate.query.service;

import com.teslamate.query.config.QueryProperties;
import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.dao.SettingsDao;
import com.teslamate.query.dto.ChargeType;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.SettingsEntity;
import com.teslamate.query.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecentActivityServiceTest {

    @Mock private CarDao carDao;
    @Mock private DriveDao driveDao;
    @Mock private PositionDao positionDao;
    @Mock private ChargingProcessDao chargingProcessDao;
    @Mock private ChargeDao chargeDao;
    @Mock private SettingsDao settingsDao;

    private RecentActivityService service;

    @BeforeEach
    void setUp() {
        QueryProperties props = new QueryProperties();
        service = new RecentActivityService(
                carDao, driveDao, chargingProcessDao, chargeDao, positionDao, settingsDao, new QuerySupport(props));
        org.mockito.Mockito.lenient().when(positionDao.findByIds(ArgumentMatchers.any())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(carDao.findById(1L)).thenReturn(Optional.of(car()));
    }

    @Test
    void recentDrivesComputesEnergyFromRangeAndEfficiency() {
        when(driveDao.findIds(ArgumentMatchers.any(), anyInt(), anyInt())).thenReturn(List.of(10L));
        when(driveDao.findByIdsOrdered(List.of(10L))).thenReturn(List.of(drive(10L, 17.0, 25,
                "184.59", "163.90")));
        when(settingsDao.find()).thenReturn(Optional.of(settings("rated")));

        var rows = service.recentDrives(1L, 5, 0);
        assertEquals(1, rows.size());
        assertEquals(10L, rows.getFirst().id());
        assertEquals(17.0, rows.getFirst().distanceKm());
        assertEquals(20.7, rows.getFirst().rangeUsedKm());
        assertEquals(184.6, rows.getFirst().startRangeKm());
        assertEquals(163.9, rows.getFirst().endRangeKm());
        assertEquals(2.86, rows.getFirst().energyUsedKwh());
        assertEquals(40.8, rows.getFirst().avgSpeedKmh());
        assertEquals(168.0, rows.getFirst().efficiencyWhKm());
    }

    @Test
    void recentChargesIncludeSocAndDc() {
        when(chargingProcessDao.findIds(ArgumentMatchers.any(), anyInt(), anyInt())).thenReturn(List.of(365L));
        when(chargingProcessDao.findByIdsOrdered(List.of(365L))).thenReturn(List.of(process()));
        when(chargeDao.findLatestPerProcess(List.of(365L))).thenReturn(List.of(sample()));
        when(chargeDao.findByProcessIds(List.of(365L), 10_000)).thenReturn(List.of());
        when(settingsDao.find()).thenReturn(Optional.of(settings("rated")));

        var rows = service.recentCharges(1L, 5);
        assertEquals(1, rows.size());
        assertEquals(44, rows.getFirst().startSocPercent());
        assertEquals(99, rows.getFirst().endSocPercent());
        assertEquals(ChargeType.DC, rows.getFirst().chargeType());
        assertEquals(new BigDecimal("31.72"), rows.getFirst().energyAddedKwh());
        assertEquals(95.4, rows.getFirst().efficiencyPercent());
        assertEquals(48.8, rows.getFirst().avgPowerKw());
    }

    @Test
    void unknownCar404() {
        when(carDao.findById(9L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.recentDrives(9L, 5, 0));
    }

    private static CarEntity car() {
        return new CarEntity(1L, 1L, 1L, "VIN", null, "Y", 0.13826, "50", "SR",
                "StealthGrey", "Apollo19", null, 1, 1L);
    }

    private static SettingsEntity settings(String range) {
        return new SettingsEntity(1L, "km", "C", "bar", range, null, null, "zh");
    }

    private static DriveEntity drive(long id, double distance, int duration, String startRange, String endRange) {
        BigDecimal s = new BigDecimal(startRange);
        BigDecimal e = new BigDecimal(endRange);
        return new DriveEntity(
                id, 1L,
                Instant.parse("2026-08-13T07:10:58Z"), Instant.parse("2026-08-13T07:35:44Z"),
                null, null, null, null, null,
                s, e, s, e,
                0.0, distance, distance, duration, null, null,
                null, null, null, null, null, null);
    }

    private static ChargingProcessEntity process() {
        return new ChargingProcessEntity(
                365L, 1L,
                Instant.parse("2026-08-01T16:05:46Z"), Instant.parse("2026-08-01T16:44:20Z"),
                new BigDecimal("31.72"), new BigDecimal("33.26"),
                new BigDecimal("185.46"), new BigDecimal("414.89"),
                new BigDecimal("185.46"), new BigDecimal("414.89"),
                44, 99, 39, null, null, null, null, null);
    }

    private static ChargeEntity sample() {
        return new ChargeEntity(
                1L, 365L, Instant.parse("2026-08-01T16:20:00Z"),
                99, 99, new BigDecimal("31.72"),
                80, 400, 200, 3,
                true, "Gb", "GB_DC",
                new BigDecimal("414.89"), new BigDecimal("414.89"),
                new BigDecimal("28"), false);
    }
}
