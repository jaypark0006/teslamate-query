package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.CarSettingsDao;
import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.entity.CarSettingsEntity;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CarService {

    private final CarDao carDao;
    private final CarSettingsDao carSettingsDao;

    public CarService(CarDao carDao, CarSettingsDao carSettingsDao) {
        this.carDao = carDao;
        this.carSettingsDao = carSettingsDao;
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

    public LatestSnapshotDto latest(long carId) {
        get(carId);
        return carDao.findLatest(carId)
                .orElseThrow(() -> new NotFoundException("No telemetry found for car: " + carId));
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
                .collect(Collectors.toMap(CarSettingsEntity::id, Function.identity(), (a, b) -> a));
    }
}
