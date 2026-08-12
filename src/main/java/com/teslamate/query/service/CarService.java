package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.CarSettingsDao;
import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.db.condition.PositionSearchCondition;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.entity.CarSettingsEntity;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CarService {

    private final CarDao carDao;
    private final CarSettingsDao carSettingsDao;
    private final PositionDao positionDao;
    private final ChargeDao chargeDao;
    private final ChargingProcessDao chargingProcessDao;

    public CarService(CarDao carDao, CarSettingsDao carSettingsDao, PositionDao positionDao,
                      ChargeDao chargeDao, ChargingProcessDao chargingProcessDao) {
        this.carDao = carDao;
        this.carSettingsDao = carSettingsDao;
        this.positionDao = positionDao;
        this.chargeDao = chargeDao;
        this.chargingProcessDao = chargingProcessDao;
    }

    @Cacheable("cars")
    public List<CarDto> list() {
        List<CarEntity> cars = carDao.findAll();
        Map<Long, CarSettingsEntity> settingsById = loadSettings(cars);
        return cars.stream()
                .map(c -> EntityMapper.toCarDto(c, settingsById.get(c.settingsId())))
                .toList();
    }

    @Cacheable(value = "cars", key = "#id")
    public CarDto get(long id) {
        CarEntity car = carDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Car not found: " + id));
        CarSettingsEntity settings = car.settingsId() == null
                ? null
                : carSettingsDao.findById(car.settingsId()).orElse(null);
        return EntityMapper.toCarDto(car, settings);
    }

    public LatestSnapshotDto latest(long carId, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        get(carId);

        PositionSearchCondition posCond = PositionSearchCondition.builder()
                .carId(carId)
                .cleanOnly(true)
                .newestFirst()
                .build();
        List<Long> posIds = positionDao.findIds(posCond, 1, 0);
        PositionEntity latestPos = posIds.isEmpty() ? null : positionDao.findById(posIds.getFirst()).orElse(null);

        List<Long> processIds = chargingProcessDao.findIds(
                ChargingProcessSearchCondition.builder().carId(carId).build(), 50, 0);
        ChargeEntity latestCharge = chargeDao.findLatestByProcessIds(processIds).orElse(null);
        PositionEntity chargePos = null;
        if (latestCharge != null) {
            ChargingProcessEntity process = chargingProcessDao.findById(latestCharge.chargingProcessId()).orElse(null);
            if (process != null && process.positionId() != null) {
                chargePos = positionDao.findById(process.positionId()).orElse(null);
            }
        }

        LatestSnapshotDto fromPos = latestPos == null ? null : EntityMapper.fromPosition(latestPos);
        LatestSnapshotDto fromCharge = latestCharge == null
                ? null
                : EntityMapper.fromCharge(latestCharge, carId, chargePos);
        LatestSnapshotDto raw = newer(fromPos, fromCharge);
        if (raw == null) {
            throw new NotFoundException("No telemetry found for car: " + carId);
        }
        return EntityMapper.toLatestSnapshotDto(raw, u);
    }

    private static LatestSnapshotDto newer(LatestSnapshotDto a, LatestSnapshotDto b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        Instant ad = a.date();
        Instant bd = b.date();
        if (ad == null) {
            return b;
        }
        if (bd == null) {
            return a;
        }
        return ad.isAfter(bd) ? a : b;
    }

    private Map<Long, CarSettingsEntity> loadSettings(List<CarEntity> cars) {
        List<Long> settingsIds = cars.stream()
                .map(CarEntity::settingsId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (settingsIds.isEmpty()) {
            return Map.of();
        }
        return carSettingsDao.findByIds(settingsIds).stream()
                .collect(Collectors.toMap(CarSettingsEntity::id, Function.identity(), (x, y) -> x));
    }
}
