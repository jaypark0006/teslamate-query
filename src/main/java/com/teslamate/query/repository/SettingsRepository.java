package com.teslamate.query.repository;

import com.teslamate.query.dto.SettingsDto;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SettingsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SettingsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SettingsDto> find() {
        String sql = """
                SELECT id, unit_of_length, unit_of_temperature, unit_of_pressure,
                       preferred_range, base_url, grafana_url, language, theme_mode
                FROM settings
                ORDER BY id
                LIMIT 1
                """;
        List<SettingsDto> list = jdbc.query(sql, (rs, i) -> new SettingsDto(
                rs.getLong("id"),
                rs.getString("unit_of_length"),
                rs.getString("unit_of_temperature"),
                rs.getString("unit_of_pressure"),
                rs.getString("preferred_range"),
                rs.getString("base_url"),
                rs.getString("grafana_url"),
                rs.getString("language"),
                rs.getString("theme_mode")
        ));
        return list.stream().findFirst();
    }
}
