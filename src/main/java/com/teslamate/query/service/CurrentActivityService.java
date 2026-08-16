package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.dao.SettingsDao;
import com.teslamate.query.dao.UpdateDao;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.db.condition.UpdateSearchCondition;
import com.teslamate.query.dto.ActivityStatus;
import com.teslamate.query.dto.CurrentChargingDto;
import com.teslamate.query.dto.CurrentDriveDto;
import com.teslamate.query.dto.CurrentParkingDto;
import com.teslamate.query.dto.CurrentStatusDto;
import com.teslamate.query.dto.TirePressureDto;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.SettingsEntity;
import com.teslamate.query.entity.UpdateEntity;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CurrentActivityService {

    private static final int ONE = 1;

    private final CarDao carDao;
    private final DriveDao driveDao;
    private final ChargingProcessDao chargingProcessDao;
    private final ChargeDao chargeDao;
    private final PositionDao positionDao;
    private final UpdateDao updateDao;
    private final SettingsDao settingsDao;
    private final Clock clock;

    public CurrentActivityService(
            CarDao carDao,
            DriveDao driveDao,
            ChargingProcessDao chargingProcessDao,
            ChargeDao chargeDao,
            PositionDao positionDao,
            UpdateDao updateDao,
            SettingsDao settingsDao,
            Clock clock
    ) {
        this.carDao = carDao;
        this.driveDao = driveDao;
        this.chargingProcessDao = chargingProcessDao;
        this.chargeDao = chargeDao;
        this.positionDao = positionDao;
        this.updateDao = updateDao;
        this.settingsDao = settingsDao;
        this.clock = clock;
    }

    @Cacheable(value = "currentStatus", key = "#carId", sync = true)
    public CurrentStatusDto status(long carId) {
        Snapshot snap = load(carId);
        PositionEntity pos = snap.latestPosition.orElse(null);
        PositionEntity tpmsPos = tpmsPosition(carId, pos);
        String preferred = preferredRangeMode();
        return new CurrentStatusDto(
                carId,
                snap.car.model(),
                snap.car.wheelType(),
                snap.status,
                ActivityClassifier.minutesBetween(snap.statusSince, snap.now),
                pos == null ? null : pos.batteryLevel(),
                pos == null ? null : pickRange(pos.idealBatteryRangeKm(), pos.ratedBatteryRangeKm(), preferred),
                pos == null ? null : pos.odometer(),
                pos == null ? null : pos.longitude(),
                pos == null ? null : pos.latitude(),
                pos == null ? null : pos.insideTemp(),
                pos == null ? null : pos.outsideTemp(),
                pos == null ? null : pos.climateOn(),
                pos == null ? null : pos.driverTempSetting(),
                tirePressure(tpmsPos),
                tpmsPos == null ? null : tpmsPos.date(),
                latestFirmware(carId)
        );
    }

    @Cacheable(value = "currentCharging", key = "#carId", sync = true)
    public CurrentChargingDto charging(long carId) {
        Snapshot snap = load(carId);
        ChargingProcessEntity process = snap.openCharge
                .orElseThrow(() -> new NotFoundException("No in-progress charging for car: " + carId));
        ChargeEntity sample = chargeDao.findLatestByProcessIds(List.of(process.id())).orElse(null);
        String preferred = preferredRangeMode();
        BigDecimal energy = process.chargeEnergyAdded();
        if (energy == null && sample != null) {
            energy = sample.chargeEnergyAdded();
        }
        return new CurrentChargingDto(
                carId,
                process.id(),
                energy,
                process.startBatteryLevel(),
                pickRange(process.startIdealRangeKm(), process.startRatedRangeKm(), preferred),
                sample == null
                        ? null
                        : ActivityClassifier.chargeType(
                                sample.fastChargerPresent(), sample.fastChargerType(), sample.connChargeCable()),
                process.cost()
        );
    }

    @Cacheable(value = "currentDrive", key = "#carId", sync = true)
    public CurrentDriveDto drive(long carId) {
        Snapshot snap = load(carId);
        DriveEntity drive = snap.openDrive
                .orElseThrow(() -> new NotFoundException("No in-progress drive for car: " + carId));
        PositionEntity pos = latestPositionForDrive(drive.id()).or(() -> snap.latestPosition).orElse(null);
        String preferred = preferredRangeMode();
        BigDecimal startRange = pickRange(drive.startIdealRangeKm(), drive.startRatedRangeKm(), preferred);
        BigDecimal currentRange = pos == null
                ? null
                : pickRange(pos.idealBatteryRangeKm(), pos.ratedBatteryRangeKm(), preferred);
        return new CurrentDriveDto(
                carId,
                drive.id(),
                pos == null ? null : pos.speed(),
                pos == null ? null : pos.power(),
                distanceKm(drive, pos),
                ActivityClassifier.minutesBetween(drive.startDate(), snap.now),
                rangeChange(startRange, currentRange)
        );
    }

    @Cacheable(value = "currentParking", key = "#carId", sync = true)
    public CurrentParkingDto parking(long carId) {
        Snapshot snap = load(carId);
        if (snap.status != ActivityStatus.PARKING) {
            throw new NotFoundException("Car is not parking: " + carId);
        }
        return new CurrentParkingDto(
                carId,
                ActivityClassifier.minutesBetween(snap.statusSince, snap.now),
                snap.latestPosition.map(PositionEntity::outsideTemp).orElse(null)
        );
    }

    Snapshot load(long carId) {
        CarEntity car = carDao.findById(carId)
                .orElseThrow(() -> new NotFoundException("Car not found: " + carId));
        ReadJobs.Pair<Optional<ChargingProcessEntity>, Optional<DriveEntity>> open = ReadJobs.both(
                () -> first(
                        chargingProcessDao.findIds(
                                ChargingProcessSearchCondition.builder().carId(carId).incompleteOnly(true).build(),
                                ONE, 0),
                        chargingProcessDao::findById),
                () -> first(
                        driveDao.findIds(
                                DriveSearchCondition.builder().carId(carId).incompleteOnly(true).build(),
                                ONE, 0),
                        driveDao::findById));
        Optional<ChargingProcessEntity> openCharge = open.first();
        Optional<DriveEntity> openDrive = open.second();
        ActivityStatus status = ActivityClassifier.status(openCharge, openDrive);
        Optional<DriveEntity> lastDrive = Optional.empty();
        Optional<ChargingProcessEntity> lastCharge = Optional.empty();
        ReadJobs.Pair<Optional<PositionEntity>, ReadJobs.Pair<Optional<DriveEntity>, Optional<ChargingProcessEntity>>> rest =
                ReadJobs.both(
                        () -> positionDao.findLatestByCarId(carId),
                        () -> status != ActivityStatus.PARKING
                                ? new ReadJobs.Pair<Optional<DriveEntity>, Optional<ChargingProcessEntity>>(
                                Optional.empty(), Optional.empty())
                                : ReadJobs.both(
                                () -> first(
                                        driveDao.findIds(
                                                DriveSearchCondition.builder().carId(carId)
                                                        .completedOnly(true).newestEndFirst().build(),
                                                ONE, 0),
                                        driveDao::findById),
                                () -> first(
                                        chargingProcessDao.findIds(
                                                ChargingProcessSearchCondition.builder()
                                                        .carId(carId).completedOnly(true).newestEndFirst().build(),
                                                ONE, 0),
                                        chargingProcessDao::findById)));
        Optional<PositionEntity> latestPosition = rest.first();
        if (status == ActivityStatus.PARKING) {
            lastDrive = rest.second().first();
            lastCharge = rest.second().second();
        }
        Instant now = clock.instant();
        Instant since = ActivityClassifier.statusSince(
                status, openCharge, openDrive, lastDrive, lastCharge,
                latestPosition.map(PositionEntity::date).orElse(now));
        return new Snapshot(car, status, since, now, openCharge, openDrive, latestPosition);
    }

    private PositionEntity tpmsPosition(long carId, PositionEntity latest) {
        if (hasTpms(latest)) {
            return latest;
        }
        return positionDao.findLatestWithTpmsByCarId(carId).orElse(null);
    }

    private Optional<PositionEntity> latestPositionForDrive(long driveId) {
        return positionDao.findLatestByDriveId(driveId);
    }

    private String latestFirmware(long carId) {
        return first(
                updateDao.findIds(UpdateSearchCondition.builder().carId(carId).build(), ONE, 0),
                updateDao::findById)
                .map(UpdateEntity::version)
                .orElse(null);
    }

    private String preferredRangeMode() {
        return settingsDao.find()
                .map(SettingsEntity::preferredRange)
                .orElse("rated");
    }

    static BigDecimal pickRange(BigDecimal ideal, BigDecimal rated, String preferred) {
        boolean wantIdeal = preferred != null && preferred.equalsIgnoreCase("ideal");
        if (wantIdeal && ideal != null) {
            return ideal;
        }
        return rated != null ? rated : ideal;
    }

    static Double distanceKm(DriveEntity drive, PositionEntity pos) {
        if (pos != null && pos.odometer() != null && drive.startKm() != null) {
            double delta = pos.odometer() - drive.startKm();
            if (delta >= 0) {
                return delta;
            }
        }
        return drive.distance();
    }

    static BigDecimal rangeChange(BigDecimal start, BigDecimal current) {
        if (start == null || current == null) {
            return null;
        }
        return current.subtract(start);
    }

    private static TirePressureDto tirePressure(PositionEntity p) {
        if (!hasTpms(p)) {
            return null;
        }
        return new TirePressureDto(p.tpmsPressureFl(), p.tpmsPressureFr(), p.tpmsPressureRl(), p.tpmsPressureRr());
    }

    private static boolean hasTpms(PositionEntity p) {
        return p != null && (p.tpmsPressureFl() != null
                || p.tpmsPressureFr() != null
                || p.tpmsPressureRl() != null
                || p.tpmsPressureRr() != null);
    }

    private static <T> Optional<T> first(List<Long> ids, java.util.function.Function<Long, Optional<T>> load) {
        if (ids == null || ids.isEmpty()) {
            return Optional.empty();
        }
        return load.apply(ids.getFirst());
    }

    record Snapshot(
            CarEntity car,
            ActivityStatus status,
            Instant statusSince,
            Instant now,
            Optional<ChargingProcessEntity> openCharge,
            Optional<DriveEntity> openDrive,
            Optional<PositionEntity> latestPosition
    ) {}
}
