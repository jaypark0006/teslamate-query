package com.teslamate.query.repository;

import com.teslamate.query.dao.SettingsDao;
import com.teslamate.query.dto.SettingsDto;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SettingsRepository {

    private final SettingsDao settingsDao;

    public SettingsRepository(SettingsDao settingsDao) {
        this.settingsDao = settingsDao;
    }

    public Optional<SettingsDto> find() {
        return settingsDao.find();
    }
}
