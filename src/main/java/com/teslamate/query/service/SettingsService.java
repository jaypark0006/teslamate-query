package com.teslamate.query.service;

import com.teslamate.query.dao.SettingsDao;
import com.teslamate.query.dto.SettingsDto;
import com.teslamate.query.exception.NotFoundException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final SettingsDao settingsDao;

    public SettingsService(SettingsDao settingsDao) {
        this.settingsDao = settingsDao;
    }

    @Cacheable("settings")
    public SettingsDto get() {
        return settingsDao.find()
                .map(EntityMapper::toSettingsDto)
                .orElseThrow(() -> new NotFoundException("Settings not found"));
    }
}
