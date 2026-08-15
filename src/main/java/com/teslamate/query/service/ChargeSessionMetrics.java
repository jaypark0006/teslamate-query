package com.teslamate.query.service;

import com.teslamate.query.dto.ChargePowerBand;
import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargingProcessEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/** Session-level charge quality from the process row + samples. */
public final class ChargeSessionMetrics {

    private ChargeSessionMetrics() {}

    public static Double efficiencyPercent(BigDecimal added, BigDecimal used) {
        if (added == null || used == null || used.signum() <= 0) {
            return null;
        }
        return added.multiply(BigDecimal.valueOf(100))
                .divide(used, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static Double avgPowerKw(BigDecimal added, Integer durationMin) {
        if (added == null || durationMin == null || durationMin <= 0) {
            return null;
        }
        return added.multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(durationMin), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Power over the overlap with [fromSoc, toSoc]. If the session never reaches
     * {@code toSoc}, uses the last sample and marks the band incomplete.
     */
    public static ChargePowerBand band(List<ChargeEntity> samples, int fromSoc, Integer toSoc) {
        if (samples == null || samples.size() < 2) {
            return null;
        }
        List<ChargeEntity> ordered = samples.stream()
                .filter(s -> s.date() != null && s.batteryLevel() != null)
                .sorted(Comparator.comparing(ChargeEntity::date))
                .toList();
        if (ordered.size() < 2) {
            return null;
        }
        ChargeEntity start = null;
        ChargeEntity end = null;
        boolean reachedTo = false;
        for (ChargeEntity s : ordered) {
            if (start == null && s.batteryLevel() >= fromSoc) {
                start = s;
            }
            if (toSoc != null && s.batteryLevel() >= toSoc) {
                end = s;
                reachedTo = true;
                break;
            }
        }
        if (toSoc == null) {
            end = ordered.getLast();
            reachedTo = start != null && start.batteryLevel() >= fromSoc;
        }
        if (start == null) {
            return null;
        }
        if (toSoc != null && start.batteryLevel() >= toSoc) {
            return null;
        }
        if (end == null) {
            end = ordered.getLast();
        }
        if (!end.date().isAfter(start.date())) {
            return null;
        }
        int actualFrom = start.batteryLevel();
        int actualTo = end.batteryLevel();
        boolean sawFloor = ordered.stream().anyMatch(s -> s.batteryLevel() <= fromSoc);
        boolean complete = sawFloor && (toSoc == null || reachedTo);
        Double kw = powerBetween(ordered, start, end);
        if (kw == null) {
            return null;
        }
        return new ChargePowerBand(kw, actualFrom, actualTo, complete);
    }

    public static ChargePowerBand band20to80(List<ChargeEntity> samples) {
        return band(samples, 20, 80);
    }

    public static ChargePowerBand band80toEnd(List<ChargeEntity> samples) {
        return band(samples, 80, null);
    }

    public static Double band20to80Kw(List<ChargeEntity> samples) {
        ChargePowerBand b = band20to80(samples);
        return b == null ? null : b.kw();
    }

    public static Double band80toEndKw(List<ChargeEntity> samples) {
        ChargePowerBand b = band80toEnd(samples);
        return b == null ? null : b.kw();
    }

    public static Double efficiencyPercent(ChargingProcessEntity process) {
        return process == null ? null : efficiencyPercent(process.chargeEnergyAdded(), process.chargeEnergyUsed());
    }

    public static Double avgPowerKw(ChargingProcessEntity process) {
        return process == null ? null : avgPowerKw(process.chargeEnergyAdded(), process.durationMin());
    }

    private static Double powerBetween(List<ChargeEntity> ordered, ChargeEntity start, ChargeEntity end) {
        if (start.chargeEnergyAdded() != null && end.chargeEnergyAdded() != null) {
            BigDecimal delta = end.chargeEnergyAdded().subtract(start.chargeEnergyAdded());
            if (delta.signum() > 0) {
                double hours = Duration.between(start.date(), end.date()).toMillis() / 3_600_000.0;
                if (hours > 0) {
                    return BigDecimal.valueOf(delta.doubleValue() / hours)
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();
                }
            }
        }
        return averageChargerPower(ordered, start, end);
    }

    private static Double averageChargerPower(List<ChargeEntity> ordered, ChargeEntity start, ChargeEntity end) {
        double sum = 0;
        int n = 0;
        for (ChargeEntity s : ordered) {
            if (s.date().isBefore(start.date()) || s.date().isAfter(end.date())) {
                continue;
            }
            if (s.chargerPower() != null) {
                sum += s.chargerPower();
                n++;
            }
        }
        if (n == 0) {
            return null;
        }
        return BigDecimal.valueOf(sum / n).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
