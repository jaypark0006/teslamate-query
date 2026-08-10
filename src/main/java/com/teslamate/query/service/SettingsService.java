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
        return settingsDao.find().orElseThrow(() -> new NotFoundException("Settings not found"));
    }

    /** preferred_range from DB, fallback rated (matches common TeslaMate installs). */
    public String preferredRangeOrDefault() {
        try {
            String r = get().preferredRange();
            return r == null || r.isBlank() ? "rated" : r;
        } catch (Exception e) {
            return "rated";
        }
    }
}
