package com.teslamate.query.service;

import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.repository.CarRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarService {

    private final CarRepository carRepository;

    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @Cacheable("cars")
    public List<CarDto> list() {
        return carRepository.findAll();
    }

    @Cacheable(value = "cars", key = "#id")
    public CarDto get(long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Car not found: " + id));
    }

    public LatestSnapshotDto latest(long carId) {
        get(carId);
        return carRepository.findLatest(carId)
                .orElseThrow(() -> new NotFoundException("No telemetry found for car: " + carId));
    }
}
