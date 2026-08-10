package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.dto.OverviewDto;
import com.teslamate.query.dao.StatsDao;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OverviewService {

    private final StatsDao statsDao;
    private final CarDao carDao;
    private final SettingsService settingsService;
    private final QuerySupport support;

    public OverviewService(StatsDao statsDao, CarDao carDao,
                           SettingsService settingsService, QuerySupport support) {
        this.statsDao = statsDao;
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
        var charge = statsDao.chargeEnergyAndCost(carId, from, to);

        return new OverviewDto(
                carId,
                from,
                to,
                latest,
                statsDao.totalDistance(carId, from, to),
                statsDao.netConsumptionWhPerKm(carId, from, to, rangeMode),
                statsDao.grossConsumptionWhPerKm(carId, from, to, rangeMode),
                charge.energyAdded(),
                charge.cost(),
                statsDao.driveCount(carId, from, to),
                statsDao.chargeCount(carId, from, to),
                statsDao.latestFirmware(carId),
                statsDao.lfpBattery(carId)
        );
    }
}
