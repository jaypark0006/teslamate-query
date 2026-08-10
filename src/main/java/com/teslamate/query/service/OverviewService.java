package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.dto.OverviewDto;
import com.teslamate.query.repository.StatsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OverviewService {

    private final StatsRepository statsRepository;
    private final CarDao carDao;
    private final SettingsService settingsService;
    private final QuerySupport support;

    public OverviewService(StatsRepository statsRepository, CarDao carDao,
                           SettingsService settingsService, QuerySupport support) {
        this.statsRepository = statsRepository;
        this.carDao = carDao;
        this.settingsService = settingsService;
        this.support = support;
    }

    @Cacheable(value = "overview", key = "#carId + '-' + #fromStr + '-' + #toStr + '-' + #range")
    public OverviewDto get(long carId, String fromStr, String toStr, String range) {
        Instant[] r = support.requireRange(fromStr, toStr);
        Instant from = r[0];
        Instant to = r[1];
        String rangeMode = support.rangeMode(range, settingsService.preferredRangeOrDefault());

        LatestSnapshotDto latest = carDao.findLatest(carId).orElse(null);
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
}
