package com.teslamate.query.service;

import com.teslamate.query.dao.GeofenceDao;
import com.teslamate.query.db.condition.GeofenceSearchCondition;
import com.teslamate.query.domain.units.DisplayUnits;
import com.teslamate.query.dto.GeofenceDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeofenceService {

    private final GeofenceDao geofenceDao;
    private final QuerySupport support;

    public GeofenceService(GeofenceDao geofenceDao, QuerySupport support) {
        this.geofenceDao = geofenceDao;
        this.support = support;
    }

    public PageResponse<GeofenceDto> list(String name, Integer page, Integer size, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        GeofenceSearchCondition condition = GeofenceSearchCondition.builder().name(name).build();
        int p = support.page(page);
        int s = support.size(size);
        long total = geofenceDao.count(condition);
        List<Long> ids = geofenceDao.findIds(condition, s, support.offset(p, s));
        return PageResponse.of(
                EntityMapper.toGeofenceDtos(geofenceDao.findByIdsOrdered(ids), u), p, s, total, u.toMeta());
    }

    public GeofenceDto get(long id, DisplayUnits units) {
        DisplayUnits u = units == null ? DisplayUnits.METRIC : units;
        return geofenceDao.findById(id)
                .map(e -> EntityMapper.toGeofenceDto(e, u))
                .orElseThrow(() -> new NotFoundException("Geofence not found: " + id));
    }
}
