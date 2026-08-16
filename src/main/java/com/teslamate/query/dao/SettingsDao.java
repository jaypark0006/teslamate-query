package com.teslamate.query.dao;

import com.teslamate.query.entity.SettingsEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SettingsDao {

    private final Jdbi jdbi;

    public SettingsDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<SettingsEntity> find() {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM settings
                ORDER BY id
                LIMIT 1
                """)
                .map(ConstructorMapper.of(SettingsEntity.class))
                .findOne());
    }
}
