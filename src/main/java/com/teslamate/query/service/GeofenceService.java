package com.teslamate.query.service;

import com.teslamate.query.dao.GeofenceDao;
import com.teslamate.query.dto.GeofenceDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeofenceService {

    private final GeofenceDao geofenceDao;

    public GeofenceService(GeofenceDao geofenceDao) {
        this.geofenceDao = geofenceDao;
    }

    @Cacheable("geofences")
    public List<GeofenceDto> list() {
        return geofenceDao.findAll();
    }
}
