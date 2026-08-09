package com.teslamate.query.service;

import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.dto.OverviewDto;
import com.teslamate.query.dto.SettingsDto;
import com.teslamate.query.repository.CarRepository;
import com.teslamate.query.repository.StatsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OverviewService {

    private final StatsRepository statsRepository;
    private final CarRepository carRepository;
    private final SettingsService settingsService;
    private final QuerySupport support;

    public OverviewService(StatsRepository statsRepository, CarRepository carRepository,
                           SettingsService settingsService, QuerySupport support) {
        this.statsRepository = statsRepository;
        this.carRepository = carRepository;
        this.settingsService = settingsService;
        this.support = support;
    }

    @Cacheable(value = "overview", key = "#carId + '-' + #fromStr + '-' + #toStr + '-' + #range")
    public OverviewDto get(long carId, String fromStr, String toStr, String range) {
        Instant from = support.parseInstant(fromStr, "from");
        Instant to = support.parseInstant(toStr, "to");
        support.requireTimeRange(from, to);
        String rangeMode = resolveRange(range);

        LatestSnapshotDto latest = carRepository.findLatest(carId).orElse(null);
        var charge = statsRepository.chargeEnergyAndCost(carId, from, to);

        return new OverviewDto(
                carId,
                from,
                to,
                latest,
                statsRepository.totalDistance(carId, from, to),
                statsRepository.netConsumptionWhPerKm(carId, from, to, rangeMode),
                statsRepository.grossConsumptionWhPerKm(carId, from, to, rangeMode),
                charge.energyAdded(),
                charge.cost(),
                statsRepository.driveCount(carId, from, to),
                statsRepository.chargeCount(carId, from, to),
                statsRepository.latestFirmware(carId),
                statsRepository.lfpBattery(carId)
        );
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
