package com.teslamate.query.service;

import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.repository.DriveRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DriveService {

    private final DriveRepository driveRepository;
    private final QuerySupport support;

    public DriveService(DriveRepository driveRepository, QuerySupport support) {
        this.driveRepository = driveRepository;
        this.support = support;
    }

    public PageResponse<DriveDto> list(Long carId, String fromStr, String toStr, Double minDistance,
                                       Integer minDuration, Long geofenceId, Boolean incompleteOnly,
                                       Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        int p = support.page(page);
        int s = support.size(size);

        long total = driveRepository.count(carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly);
        List<DriveDto> data = driveRepository.find(carId, from, to, minDistance, minDuration, geofenceId,
                incompleteOnly, s, support.offset(p, s));
        return PageResponse.of(data, p, s, total);
    }

    @Cacheable(value = "drive", key = "#id")
    public DriveDto get(long id) {
        return driveRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Drive not found: " + id));
    }

    public List<DrivePositionDto> positions(long id, Integer downsampleSeconds) {
        get(id);
        return driveRepository.findPositions(id, downsampleSeconds);
    }
}
