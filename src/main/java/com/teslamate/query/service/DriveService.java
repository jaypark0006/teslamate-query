package com.teslamate.query.service;

import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.SettingsDto;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.repository.DriveRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DriveService {

    private final DriveRepository driveRepository;
    private final SettingsService settingsService;
    private final QuerySupport support;

    public DriveService(DriveRepository driveRepository, SettingsService settingsService, QuerySupport support) {
        this.driveRepository = driveRepository;
        this.settingsService = settingsService;
        this.support = support;
    }

    public PageResponse<DriveDto> list(Long carId, String fromStr, String toStr, Double minDistance,
                                       Integer minDuration, Long geofenceId, String location,
                                       Boolean incompleteOnly, String range, Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        int p = support.page(page);
        int s = support.size(size);
        String rangeMode = resolveRange(range);

        long total = driveRepository.count(carId, from, to, minDistance, minDuration, geofenceId, location, incompleteOnly);
        List<DriveDto> data = driveRepository.find(carId, from, to, minDistance, minDuration, geofenceId,
                location, incompleteOnly, rangeMode, s, support.offset(p, s));
        return PageResponse.of(data, p, s, total);
    }

    @Cacheable(value = "drive", key = "#id + '-' + #range")
    public DriveDto get(long id, String range) {
        String rangeMode = resolveRange(range);
        return driveRepository.findById(id, rangeMode)
                .orElseThrow(() -> new NotFoundException("Drive not found: " + id));
    }

    public List<DrivePositionDto> positions(long id, Integer downsampleSeconds) {
        get(id, null);
        return driveRepository.findPositions(id, downsampleSeconds);
    }

    private String resolveRange(String range) {
        String preferred = "ideal";
        try {
            SettingsDto settings = settingsService.get();
            preferred = settings.preferredRange();
        } catch (Exception ignored) {
            // settings table may be empty in test DBs
        }
        return support.rangeMode(range, preferred);
    }
}
