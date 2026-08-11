package com.teslamate.query.service;

import com.teslamate.query.dao.CarSettingsDao;
import com.teslamate.query.dto.CarSettingsDto;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarSettingsService {

    private final CarSettingsDao carSettingsDao;

    public CarSettingsService(CarSettingsDao carSettingsDao) {
        this.carSettingsDao = carSettingsDao;
    }

    @Cacheable("carSettings")
    public List<CarSettingsDto> list() {
        return EntityMapper.toCarSettingsDtos(carSettingsDao.findAll());
    }

    @Cacheable(value = "carSettings", key = "#id")
    public CarSettingsDto get(long id) {
        return carSettingsDao.findById(id)
                .map(EntityMapper::toCarSettingsDto)
                .orElseThrow(() -> new NotFoundException("Car settings not found: " + id));
    }
}
