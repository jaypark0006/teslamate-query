package com.teslamate.query.service;

import com.teslamate.query.dto.*;
import com.teslamate.query.exception.BadRequestException;
import com.teslamate.query.dao.StatsDao;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class StatsService {

    private final StatsDao statsDao;
    private final SettingsService settingsService;
    private final QuerySupport support;

    public StatsService(StatsDao statsDao, SettingsService settingsService, QuerySupport support) {
        this.statsDao = statsDao;
        this.settingsService = settingsService;
        this.support = support;
    }

    @Cacheable(value = "stats", key = "'drives-' + #carId + '-' + #fromStr + '-' + #toStr + '-' + #groupBy + '-' + #range")
    public DriveStatsDto driveStats(long carId, String fromStr, String toStr, String groupBy, String range) {
        Instant from = requireRange(fromStr, toStr)[0];
        Instant to = requireRange(fromStr, toStr)[1];
        String gb = normalizeGroup(groupBy);
        String rangeMode = resolveRange(range);
        return new DriveStatsDto(
                carId,
                gb,
                statsDao.driveSummary(carId, from, to, rangeMode),
                statsDao.driveBuckets(carId, from, to, gb, rangeMode)
        );
    }

    @Cacheable(value = "stats", key = "'charging-' + #carId + '-' + #fromStr + '-' + #toStr")
    public ChargingStatsDto chargingStats(long carId, String fromStr, String toStr) {
        Instant from = requireRange(fromStr, toStr)[0];
        Instant to = requireRange(fromStr, toStr)[1];
        return new ChargingStatsDto(
                carId,
                statsDao.chargingSummary(carId, from, to),
                statsDao.chargingByType(carId, from, to),
                statsDao.topStations(carId, from, to, 20)
        );
    }

    @Cacheable(value = "stats", key = "'eff-' + #carId + '-' + #fromStr + '-' + #toStr + '-' + #range")
    public EfficiencyStatsDto efficiency(long carId, String fromStr, String toStr, String range) {
        Instant from = requireRange(fromStr, toStr)[0];
        Instant to = requireRange(fromStr, toStr)[1];
        String rangeMode = resolveRange(range);
        return new EfficiencyStatsDto(
                carId,
                rangeMode,
                statsDao.netConsumptionWhPerKm(carId, from, to, rangeMode),
                statsDao.grossConsumptionWhPerKm(carId, from, to, rangeMode),
                statsDao.carEfficiency(carId),
                statsDao.totalDistance(carId, from, to),
                statsDao.efficiencyByTemp(carId, from, to, rangeMode)
        );
    }

    @Cacheable(value = "stats", key = "'period-' + #carId + '-' + #fromStr + '-' + #toStr + '-' + #period + '-' + #range")
    public PeriodStatsDto period(long carId, String fromStr, String toStr, String period, String range) {
        Instant from = requireRange(fromStr, toStr)[0];
        Instant to = requireRange(fromStr, toStr)[1];
        String p = normalizeGroup(period);
        String rangeMode = resolveRange(range);
        return new PeriodStatsDto(carId, p, statsDao.periodStats(carId, from, to, p, rangeMode));
    }

    @Cacheable(value = "stats", key = "'mileage-' + #carId + '-' + #fromStr + '-' + #toStr")
    public List<MileagePointDto> mileage(long carId, String fromStr, String toStr) {
        Instant from = requireRange(fromStr, toStr)[0];
        Instant to = requireRange(fromStr, toStr)[1];
        return statsDao.mileage(carId, from, to);
    }

    private Instant[] requireRange(String fromStr, String toStr) {
        return support.requireRange(fromStr, toStr);
    }

    private String normalizeGroup(String groupBy) {
        String g = groupBy == null ? "day" : groupBy.trim().toLowerCase(Locale.ROOT);
        return switch (g) {
            case "day", "week", "month", "year" -> g;
            default -> throw new BadRequestException("groupBy/period must be day|week|month|year");
        };
    }

    private String resolveRange(String range) {
        return support.rangeMode(range, settingsService.preferredRangeOrDefault());
    }
}
