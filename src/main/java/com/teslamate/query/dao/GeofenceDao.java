package com.teslamate.query.dao;

import com.teslamate.query.dto.GeofenceDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(GeofenceDto.class)
public interface GeofenceDao {

    @SqlQuery("""
            SELECT id, name, latitude, longitude, radius,
                   billing_type, cost_per_unit, session_fee
            FROM geofences
            ORDER BY name
            """)
    List<GeofenceDto> findAll();

    @SqlQuery("""
            SELECT id, name, latitude, longitude, radius,
                   billing_type, cost_per_unit, session_fee
            FROM geofences WHERE id = :id
            """)
    Optional<GeofenceDto> findById(@Bind("id") long id);

    @SqlQuery("""
            SELECT id, name, latitude, longitude, radius,
                   billing_type, cost_per_unit, session_fee
            FROM geofences WHERE id IN (<ids>)
            """)
    List<GeofenceDto> findByIds(@BindList("ids") Collection<Long> ids);
}
