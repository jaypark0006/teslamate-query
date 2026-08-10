package com.teslamate.query.dao;

import com.teslamate.query.dto.SettingsDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.Optional;

@RegisterConstructorMapper(SettingsDto.class)
public interface SettingsDao {

    @SqlQuery("""
            SELECT id, unit_of_length, unit_of_temperature, unit_of_pressure,
                   preferred_range, base_url, grafana_url, language
            FROM settings
            ORDER BY id
            LIMIT 1
            """)
    Optional<SettingsDto> find();
}
