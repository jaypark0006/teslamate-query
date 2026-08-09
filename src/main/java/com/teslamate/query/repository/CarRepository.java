package com.teslamate.query.repository;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CarRepository {

    private final CarDao carDao;

    public CarRepository(CarDao carDao) {
        this.carDao = carDao;
    }

    public List<CarDto> findAll() {
        return carDao.findAll();
    }

    public Optional<CarDto> findById(long id) {
        return carDao.findById(id);
    }

    public Optional<LatestSnapshotDto> findLatest(long carId) {
        return carDao.findLatest(carId);
    }
}
