package com.teslamate.query.dao;

import com.teslamate.query.entity.SettingsEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** settings table (global TeslaMate settings, usually one row). */
@Repository
public class SettingsDao {

    private final Jdbi jdbi;

    public SettingsDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<SettingsEntity> find() {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT * FROM settings ORDER BY id LIMIT 1")
                .map(ConstructorMapper.of(SettingsEntity.class))
                .findOne());
    }

    public Optional<SettingsEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM settings WHERE id = :id")
                .bind("id", id)
                .map(ConstructorMapper.of(SettingsEntity.class))
                .findOne());
    }

    public List<SettingsEntity> findAll() {
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM settings ORDER BY id")
                .map(ConstructorMapper.of(SettingsEntity.class))
                .list());
    }
}
