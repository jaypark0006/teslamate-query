package com.teslamate.query.service;

import com.teslamate.query.dto.*;
import com.teslamate.query.repository.AdvancedRepository;
import com.teslamate.query.repository.StatsRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdvancedQueryService {

    private final AdvancedRepository advancedRepository;
    private final StatsRepository statsRepository;
    private final DriveService driveService;
    private final ChargingProcessService chargingProcessService;
    private final SettingsService settingsService;
    private final QuerySupport support;

    public AdvancedQueryService(AdvancedRepository advancedRepository,
                                StatsRepository statsRepository,
                                DriveService driveService,
                                ChargingProcessService chargingProcessService,
                                SettingsService settingsService,
                                QuerySupport support) {
        this.advancedRepository = advancedRepository;
        this.statsRepository = statsRepository;
        this.driveService = driveService;
        this.chargingProcessService = chargingProcessService;
        this.settingsService = settingsService;
        this.support = support;
    }

    public List<StateDto> states(long carId, String fromStr, String toStr) {
        Instant[] r = require(fromStr, toStr);
        return advancedRepository.states(carId, r[0], r[1]);
    }

    public List<UpdateDto> updates(long carId, String fromStr, String toStr) {
        Instant[] r = require(fromStr, toStr);
        return advancedRepository.updates(carId, r[0], r[1]);
    }

    public List<TimelineEventDto> timeline(long carId, String fromStr, String toStr) {
        Instant[] r = require(fromStr, toStr);
        return advancedRepository.timeline(carId, r[0], r[1]);
    }

    public TripSummaryDto tripSummary(long carId, String fromStr, String toStr, String range) {
        Instant[] r = require(fromStr, toStr);
        String rangeMode = resolveRange(range);
        PageResponse<?> drivesPage = driveService.list(carId, fromStr, toStr, null, null, null, null, null, null, 1, 200);
        @SuppressWarnings("unchecked")
        List<DriveDto> driveRows = (List<DriveDto>) (List<?>) drivesPage.data();
        PageResponse<?> chargesPage = chargingProcessService.list(carId, fromStr, toStr, null, null, null, null, null, 1, 200);
        @SuppressWarnings("unchecked")
        List<ChargingProcessDto> chargeRows = (List<ChargingProcessDto>) (List<?>) chargesPage.data();
        var chargeAgg = statsRepository.chargeEnergyAndCost(carId, r[0], r[1]);
        double duration = driveRows.stream()
                .map(DriveDto::durationMin)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .sum();
        return new TripSummaryDto(
                carId,
                r[0],
                r[1],
                statsRepository.totalDistance(carId, r[0], r[1]),
                statsRepository.driveCount(carId, r[0], r[1]),
                statsRepository.chargeCount(carId, r[0], r[1]),
                chargeAgg.energyAdded(),
                chargeAgg.cost(),
                statsRepository.netConsumptionWhPerKm(carId, r[0], r[1], rangeMode),
                duration,
                driveRows,
                chargeRows
        );
    }

    public VampireDrainDto vampireDrain(long carId, String fromStr, String toStr, String range) {
        Instant[] r = require(fromStr, toStr);
        String rangeMode = resolveRange(range);
        List<VampireDrainDto.Segment> segments = advancedRepository.vampireSegments(carId, r[0], r[1], rangeMode);
        double totalLoss = segments.stream()
                .map(VampireDrainDto.Segment::rangeLossKm)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        double totalHours = segments.stream()
                .map(VampireDrainDto.Segment::hours)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        Double avg = totalHours > 0 ? totalLoss / totalHours : null;
        return new VampireDrainDto(carId, r[0], r[1], segments, totalLoss, avg);
    }

    public ProjectedRangeDto projectedRange(long carId, String fromStr, String toStr, String range) {
        Instant[] r = require(fromStr, toStr);
        String rangeMode = resolveRange(range);
        List<ProjectedRangeDto.Point> raw = advancedRepository.projectedRange(carId, r[0], r[1], rangeMode);
        List<ProjectedRangeDto.Point> points = raw;
        if (raw.size() > 5000) {
            int stride = (int) Math.ceil(raw.size() / 5000.0);
            java.util.ArrayList<ProjectedRangeDto.Point> sampled = new java.util.ArrayList<>();
            for (int i = 0; i < raw.size(); i += stride) {
                sampled.add(raw.get(i));
            }
            points = sampled;
        }
        return new ProjectedRangeDto(carId, rangeMode, points);
    }

    public BatteryHealthDto batteryHealth(long carId, String fromStr, String toStr, String range) {
        Instant[] r = require(fromStr, toStr);
        String rangeMode = resolveRange(range);
        List<BatteryHealthDto.CapacityPoint> points = advancedRepository.batteryCapacity(carId, r[0], r[1], rangeMode);
        Double max = points.stream()
                .map(BatteryHealthDto.CapacityPoint::capacityKwh)
                .filter(v -> v != null && v > 0)
                .mapToDouble(Double::doubleValue)
                .max()
                .stream().boxed().findFirst().orElse(null);
        Double current = points.isEmpty() ? null : points.getLast().capacityKwh();
        // use last non-null
        for (int i = points.size() - 1; i >= 0; i--) {
            if (points.get(i).capacityKwh() != null) {
                current = points.get(i).capacityKwh();
                break;
            }
        }
        Double degradation = (max != null && current != null && max > 0)
                ? (1.0 - current / max) * 100.0
                : null;
        return new BatteryHealthDto(carId, rangeMode, current, max, degradation, points);
    }

    public LocationStatsDto locations(long carId, String fromStr, String toStr) {
        Instant[] r = require(fromStr, toStr);
        return new LocationStatsDto(carId, advancedRepository.locationStats(carId, r[0], r[1]));
    }

    public PageResponse<PositionDto> positions(long carId, String fromStr, String toStr, Boolean cleanOnly,
                                               Integer downsample, Integer page, Integer size) {
        Instant[] r = require(fromStr, toStr);
        boolean clean = cleanOnly == null || cleanOnly;
        int p = support.page(page);
        int s = support.size(size);
        long total = advancedRepository.countPositions(carId, r[0], r[1], clean);
        List<PositionDto> data = advancedRepository.positions(carId, r[0], r[1], clean, downsample, s, support.offset(p, s));
        return PageResponse.of(data, p, s, total);
    }

    private Instant[] require(String fromStr, String toStr) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        support.requireTimeRange(from, to);
        return new Instant[]{from, to};
    }

    private String resolveRange(String range) {
        String preferred = "ideal";
        try {
            preferred = settingsService.get().preferredRange();
        } catch (Exception ignored) {
        }
        return support.rangeMode(range, preferred);
    }
}
