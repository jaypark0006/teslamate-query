package com.teslamate.query.service;

import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DriveEnrichedDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.dto.SettingsDto;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.repository.DriveRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

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

    public PageResponse<?> list(Long carId, String fromStr, String toStr, Double minDistance,
                                Integer minDuration, Long geofenceId, Boolean incompleteOnly,
                                String view, String range, Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        int p = support.page(page);
        int s = support.size(size);
        long total = driveRepository.count(carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly);

        if (isEnriched(view)) {
            String rangeMode = resolveRange(range);
            List<DriveEnrichedDto> data = driveRepository.findEnriched(
                    carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly,
                    rangeMode, s, support.offset(p, s));
            return PageResponse.of(data, p, s, total);
        }

        List<DriveDto> data = driveRepository.find(carId, from, to, minDistance, minDuration, geofenceId,
                incompleteOnly, s, support.offset(p, s));
        return PageResponse.of(data, p, s, total);
    }

    public Object get(long id, String view, String range) {
        if (isEnriched(view)) {
            return driveRepository.findEnrichedById(id, resolveRange(range))
                    .orElseThrow(() -> new NotFoundException("Drive not found: " + id));
        }
        return getLean(id);
    }

    @Cacheable(value = "drive", key = "#id")
    public DriveDto getLean(long id) {
        return driveRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Drive not found: " + id));
    }

    public List<DrivePositionDto> positions(long id, Integer downsampleSeconds) {
        getLean(id);
        return driveRepository.findPositions(id, downsampleSeconds);
    }

    private boolean isEnriched(String view) {
        if (view == null || view.isBlank() || "lean".equalsIgnoreCase(view)) {
            return false;
        }
        if ("enriched".equalsIgnoreCase(view)) {
            return true;
        }
        throw new BadRequestException("view must be lean or enriched");
    }

    private String resolveRange(String range) {
        String preferred = "ideal";
        try {
            SettingsDto settings = settingsService.get();
            preferred = settings.preferredRange();
        } catch (Exception ignored) {
        }
        return support.rangeMode(range, preferred);
    }
}
