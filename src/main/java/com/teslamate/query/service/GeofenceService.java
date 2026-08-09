package com.teslamate.query.service;

import com.teslamate.query.dto.GeofenceDto;
import com.teslamate.query.repository.GeofenceRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeofenceService {

    private final GeofenceRepository geofenceRepository;

    public GeofenceService(GeofenceRepository geofenceRepository) {
        this.geofenceRepository = geofenceRepository;
    }

    @Cacheable("geofences")
    public List<GeofenceDto> list() {
        return geofenceRepository.findAll();
    }
}
