package com.teslamate.query.service;

import com.teslamate.query.dto.SettingsDto;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.repository.SettingsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Cacheable("settings")
    public SettingsDto get() {
        return settingsRepository.find()
                .orElseThrow(() -> new NotFoundException("Settings not found"));
    }
}
