package com.teslamate.query.repository;

import com.teslamate.query.dto.GeofenceDto;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GeofenceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public GeofenceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<GeofenceDto> findAll() {
        String sql = """
                SELECT id, name, latitude, longitude, radius,
                       billing_type, cost_per_unit, session_fee
                FROM geofences
                ORDER BY name
                """;
        return jdbc.query(sql, (rs, i) -> new GeofenceDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getInt("radius"),
                rs.getString("billing_type"),
                rs.getBigDecimal("cost_per_unit"),
                rs.getBigDecimal("session_fee")
        ));
    }
}
