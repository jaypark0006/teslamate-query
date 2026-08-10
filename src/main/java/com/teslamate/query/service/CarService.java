package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarService {

    private final CarDao carDao;

    public CarService(CarDao carDao) {
        this.carDao = carDao;
    }

    @Cacheable("cars")
    public List<CarDto> list() {
        return carDao.findAll();
    }

    @Cacheable(value = "cars", key = "#id")
    public CarDto get(long id) {
        return carDao.findById(id).orElseThrow(() -> new NotFoundException("Car not found: " + id));
    }

    public LatestSnapshotDto latest(long carId) {
        get(carId);
        return carDao.findLatest(carId)
                .orElseThrow(() -> new NotFoundException("No telemetry found for car: " + carId));
    }
}
