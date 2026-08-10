package com.teslamate.query.service;

import com.teslamate.query.dao.AnalyticsDao;
import com.teslamate.query.dao.StatsDao;
import com.teslamate.query.dao.StateDao;
import com.teslamate.query.dao.UpdateDao;
import com.teslamate.query.dto.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdvancedQueryService {

    private final AnalyticsDao analyticsDao;
    private final StatsDao statsDao;
    private final StateDao stateDao;
    private final UpdateDao updateDao;
    private final DriveService driveService;
    private final ChargingProcessService chargingProcessService;
    private final SettingsService settingsService;
    private final QuerySupport support;

    public AdvancedQueryService(AnalyticsDao analyticsDao, StatsDao statsDao, StateDao stateDao,
                                UpdateDao updateDao, DriveService driveService,
                                ChargingProcessService chargingProcessService,
                                SettingsService settingsService, QuerySupport support) {
        this.analyticsDao = analyticsDao;
        this.statsDao = statsDao;
        this.stateDao = stateDao;
        this.updateDao = updateDao;
        this.driveService = driveService;
        this.chargingProcessService = chargingProcessService;
        this.settingsService = settingsService;
        this.support = support;
    }

    public List<StateDto> states(long carId, String fromStr, String toStr) {
        Instant[] r = support.requireRange(fromStr, toStr);
        return stateDao.findByCarAndTime(carId, r[0], r[1]);
    }

    public List<UpdateDto> updates(long carId, String fromStr, String toStr) {
        Instant[] r = support.requireRange(fromStr, toStr);
        return updateDao.findByCarAndTime(carId, r[0], r[1]);
    }

    public List<TimelineEventDto> timeline(long carId, String fromStr, String toStr) {
        Instant[] r = support.requireRange(fromStr, toStr);
        return analyticsDao.timeline(carId, r[0], r[1]);
    }

    public TripSummaryDto tripSummary(long carId, String fromStr, String toStr, String range) {
        Instant[] r = support.requireRange(fromStr, toStr);
        String rangeMode = support.rangeMode(range, settingsService.preferredRangeOrDefault());
        // multi-dao: list uses findIds → findByIds inside services
        List<DriveDto> driveRows = driveService.list(carId, fromStr, toStr, null, null, null, null, 1, 200).data();
        List<ChargingProcessDto> chargeRows =
                chargingProcessService.list(carId, fromStr, toStr, null, null, 1, 200).data();
        var chargeAgg = statsDao.chargeEnergyAndCost(carId, r[0], r[1]);
        double duration = driveRows.stream()
                .map(DriveDto::durationMin)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .sum();
        return new TripSummaryDto(
                carId, r[0], r[1],
                statsDao.totalDistance(carId, r[0], r[1]),
                statsDao.driveCount(carId, r[0], r[1]),
                statsDao.chargeCount(carId, r[0], r[1]),
                chargeAgg.energyAdded(),
                chargeAgg.cost(),
                statsDao.netConsumptionWhPerKm(carId, r[0], r[1], rangeMode),
                duration,
                driveRows,
                chargeRows
        );
    }

    public VampireDrainDto vampireDrain(long carId, String fromStr, String toStr, String range) {
        Instant[] r = support.requireRange(fromStr, toStr);
        String rangeMode = support.rangeMode(range, settingsService.preferredRangeOrDefault());
        List<VampireDrainDto.Segment> segments = analyticsDao.vampireSegments(carId, r[0], r[1], rangeMode);
        double totalLoss = segments.stream().map(VampireDrainDto.Segment::rangeLossKm)
                .filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        double totalHours = segments.stream().map(VampireDrainDto.Segment::hours)
                .filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
        Double avg = totalHours > 0 ? totalLoss / totalHours : null;
        return new VampireDrainDto(carId, r[0], r[1], segments, totalLoss, avg);
    }

    public ProjectedRangeDto projectedRange(long carId, String fromStr, String toStr, String range) {
        Instant[] r = support.requireRange(fromStr, toStr);
        String rangeMode = support.rangeMode(range, settingsService.preferredRangeOrDefault());
        List<ProjectedRangeDto.Point> raw = analyticsDao.projectedRange(carId, r[0], r[1], rangeMode);
        if (raw.size() <= 5000) {
            return new ProjectedRangeDto(carId, rangeMode, raw);
        }
        int stride = (int) Math.ceil(raw.size() / 5000.0);
        java.util.ArrayList<ProjectedRangeDto.Point> sampled = new java.util.ArrayList<>();
        for (int i = 0; i < raw.size(); i += stride) {
            sampled.add(raw.get(i));
        }
        return new ProjectedRangeDto(carId, rangeMode, sampled);
    }

    public BatteryHealthDto batteryHealth(long carId, String fromStr, String toStr, String range) {
        Instant[] r = support.requireRange(fromStr, toStr);
        String rangeMode = support.rangeMode(range, settingsService.preferredRangeOrDefault());
        List<BatteryHealthDto.CapacityPoint> points = analyticsDao.batteryCapacity(carId, r[0], r[1], rangeMode);
        Double max = points.stream().map(BatteryHealthDto.CapacityPoint::capacityKwh)
                .filter(v -> v != null && v > 0).mapToDouble(Double::doubleValue).max()
                .stream().boxed().findFirst().orElse(null);
        Double current = null;
        for (int i = points.size() - 1; i >= 0; i--) {
            if (points.get(i).capacityKwh() != null) {
                current = points.get(i).capacityKwh();
                break;
            }
        }
        Double degradation = (max != null && current != null && max > 0)
                ? (1.0 - current / max) * 100.0 : null;
        return new BatteryHealthDto(carId, rangeMode, current, max, degradation, points);
    }

    public LocationStatsDto locations(long carId, String fromStr, String toStr) {
        Instant[] r = support.requireRange(fromStr, toStr);
        return new LocationStatsDto(carId, analyticsDao.locationStats(carId, r[0], r[1]));
    }

    public PageResponse<PositionDto> positions(long carId, String fromStr, String toStr, Boolean cleanOnly,
                                               Integer downsample, Integer page, Integer size) {
        Instant[] r = support.requireRange(fromStr, toStr);
        boolean clean = cleanOnly == null || cleanOnly;
        int p = support.page(page);
        int s = support.size(size);
        long total = analyticsDao.countPositions(carId, r[0], r[1], clean);
        List<PositionDto> data = analyticsDao.positions(carId, r[0], r[1], clean, downsample, s, support.offset(p, s));
        return PageResponse.of(data, p, s, total);
    }
}
