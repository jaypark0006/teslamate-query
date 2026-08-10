package com.teslamate.query.service;

import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DriveEnrichedDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PageResponse;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.exception.NotFoundException;
import com.teslamate.query.repository.DriveRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DriveService {

    private final DriveDao driveDao;
    private final PositionDao positionDao;
    private final DriveRepository driveRepository; // enriched only (legacy join)
    private final SettingsService settingsService;
    private final QuerySupport support;

    public DriveService(DriveDao driveDao, PositionDao positionDao, DriveRepository driveRepository,
                        SettingsService settingsService, QuerySupport support) {
        this.driveDao = driveDao;
        this.positionDao = positionDao;
        this.driveRepository = driveRepository;
        this.settingsService = settingsService;
        this.support = support;
    }

    /** Grafana Drives table: id filter then load rows. */
    public PageResponse<DriveDto> listLean(Long carId, String fromStr, String toStr, Double minDistance,
                                           Integer minDuration, Long geofenceId, Boolean incompleteOnly,
                                           Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        int p = support.page(page);
        int s = support.size(size);
        long total = driveDao.count(carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly);
        List<Long> ids = driveDao.findIds(carId, from, to, minDistance, minDuration, geofenceId,
                incompleteOnly, s, support.offset(p, s));
        List<DriveDto> data = driveDao.findByIdsOrdered(ids);
        return PageResponse.of(data, p, s, total);
    }

    public PageResponse<DriveEnrichedDto> listEnriched(Long carId, String fromStr, String toStr, Double minDistance,
                                                       Integer minDuration, Long geofenceId, Boolean incompleteOnly,
                                                       String range, Integer page, Integer size) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        int p = support.page(page);
        int s = support.size(size);
        String rangeMode = support.rangeMode(range, settingsService.preferredRangeOrDefault());
        long total = driveDao.count(carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly);
        List<DriveEnrichedDto> data = driveRepository.findEnriched(
                carId, from, to, minDistance, minDuration, geofenceId, incompleteOnly,
                rangeMode, s, support.offset(p, s));
        return PageResponse.of(data, p, s, total);
    }

    @Cacheable(value = "drive", key = "#id")
    public DriveDto getLean(long id) {
        return driveDao.findById(id).orElseThrow(() -> new NotFoundException("Drive not found: " + id));
    }

    public DriveEnrichedDto getEnriched(long id, String range) {
        String rangeMode = support.rangeMode(range, settingsService.preferredRangeOrDefault());
        return driveRepository.findEnrichedById(id, rangeMode)
                .orElseThrow(() -> new NotFoundException("Drive not found: " + id));
    }

    /** Drive Details path — PositionDao only. */
    public List<DrivePositionDto> positions(long id, Integer downsampleSeconds) {
        getLean(id);
        List<DrivePositionDto> all = positionDao.findByDriveId(id);
        if (downsampleSeconds == null || downsampleSeconds <= 0 || all.size() <= 2) {
            return all;
        }
        // light client-side stride if caller asked downsample without SQL
        long bucketMs = downsampleSeconds * 1000L;
        java.util.ArrayList<DrivePositionDto> out = new java.util.ArrayList<>();
        Long lastBucket = null;
        for (DrivePositionDto pt : all) {
            if (pt.date() == null) {
                continue;
            }
            long b = pt.date().toEpochMilli() / bucketMs;
            if (lastBucket == null || b != lastBucket) {
                out.add(pt);
                lastBucket = b;
            }
        }
        if (!all.isEmpty() && (out.isEmpty() || !out.getLast().id().equals(all.getLast().id()))) {
            out.add(all.getLast());
        }
        return out;
    }

    public static boolean isEnriched(String view) {
        if (view == null || view.isBlank() || "lean".equalsIgnoreCase(view)) {
            return false;
        }
        if ("enriched".equalsIgnoreCase(view)) {
            return true;
        }
        throw new BadRequestException("view must be lean or enriched");
    }
}
