package com.teslamate.query.repository;

import com.teslamate.query.dao.GeofenceDao;
import com.teslamate.query.dto.GeofenceDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GeofenceRepository {

    private final GeofenceDao geofenceDao;

    public GeofenceRepository(GeofenceDao geofenceDao) {
        this.geofenceDao = geofenceDao;
    }

    public List<GeofenceDto> findAll() {
        return geofenceDao.findAll();
    }
}
