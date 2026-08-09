package com.teslamate.query.dao;

import com.teslamate.query.dto.GeofenceDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

@RegisterConstructorMapper(GeofenceDto.class)
public interface GeofenceDao {

    @SqlQuery("""
            SELECT id, name, latitude, longitude, radius,
                   billing_type, cost_per_unit, session_fee
            FROM geofences
            ORDER BY name
            """)
    List<GeofenceDto> findAll();
}
