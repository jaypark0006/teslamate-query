package com.teslamate.query.repository;

import com.teslamate.query.dto.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.teslamate.query.repository.CarRepository.getDouble;
import static com.teslamate.query.repository.CarRepository.getInteger;
import static com.teslamate.query.repository.CarRepository.toInstant;

@Repository
public class StatsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public StatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public MapSqlParameterSource carTime(long carId, Instant from, Instant to) {
        return new MapSqlParameterSource()
                .addValue("carId", carId)
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));
    }

    public Double netConsumptionWhPerKm(long carId, Instant from, Instant to, String rangeMode) {
        String sql = """
                SELECT
                  CASE WHEN sum(d.distance) IS NULL OR sum(d.distance) = 0 THEN NULL
                       ELSE sum((d.start_%1$s_range_km - d.end_%1$s_range_km) * car.efficiency * 1000)
                            / NULLIF(sum(d.distance), 0)
                  END AS wh_per_km
                FROM drives d
                JOIN cars car ON car.id = d.car_id
                WHERE d.car_id = :carId
                  AND d.start_date >= :from AND d.start_date <= :to
                  AND d.end_date IS NOT NULL
                """.formatted(rangeMode);
        return jdbc.query(sql, carTime(carId, from, to), rs -> rs.next() ? getDouble(rs, "wh_per_km") : null);
    }

    /**
     * Gross consumption shared pattern from Overview / Charging Stats / Efficiency / Statistics.
     * Approximates range loss around charges + odometer distance in window.
     */
    public Double grossConsumptionWhPerKm(long carId, Instant from, Instant to, String rangeMode) {
        String sql = """
                WITH d AS (
                  SELECT
                    c.car_id,
                    lag(c.end_%1$s_range_km) OVER (ORDER BY c.start_date) - c.start_%1$s_range_km AS range_loss,
                    p.odometer - lag(p.odometer) OVER (ORDER BY c.start_date) AS distance
                  FROM charging_processes c
                  LEFT JOIN positions p ON p.id = c.position_id
                  WHERE c.end_date IS NOT NULL
                    AND c.car_id = :carId
                    AND c.start_date >= :from AND c.start_date <= :to
                ),
                range_loss_between_charges AS (
                  SELECT sum(range_loss) AS range_loss
                  FROM d
                  WHERE distance >= 0 AND range_loss >= 0
                ),
                charge_dates AS (
                  SELECT min(start_date) AS first_charge, max(end_date) AS last_charge
                  FROM charging_processes
                  WHERE end_date IS NOT NULL AND car_id = :carId
                    AND start_date >= :from AND start_date <= :to
                ),
                range_loss_before_first_charge AS (
                  SELECT max(pos.%1$s_battery_range_km) - min(pos.%1$s_battery_range_km) AS range_loss
                  FROM positions pos, charge_dates
                  WHERE pos.car_id = :carId
                    AND pos.date >= :from AND pos.date <= :to
                    AND (charge_dates.first_charge IS NULL OR pos.date < charge_dates.first_charge)
                ),
                range_loss_after_last_charge AS (
                  SELECT max(pos.%1$s_battery_range_km) - min(pos.%1$s_battery_range_km) AS range_loss
                  FROM positions pos, charge_dates
                  WHERE pos.car_id = :carId
                    AND pos.date >= :from AND pos.date <= :to
                    AND charge_dates.last_charge IS NOT NULL
                    AND pos.date > charge_dates.last_charge
                ),
                total_range_loss AS (
                  SELECT sum(range_loss) AS range_loss FROM (
                    SELECT range_loss FROM range_loss_between_charges
                    UNION ALL SELECT range_loss FROM range_loss_before_first_charge
                    UNION ALL SELECT range_loss FROM range_loss_after_last_charge
                  ) r
                ),
                distance AS (
                  SELECT max(odometer) - min(odometer) AS distance
                  FROM positions
                  WHERE car_id = :carId AND date >= :from AND date <= :to
                )
                SELECT
                  CASE WHEN NULLIF(distance.distance, 0) IS NULL OR NULLIF(total_range_loss.range_loss, 0) IS NULL
                       THEN NULL
                       ELSE total_range_loss.range_loss * (cars.efficiency * 1000) / distance.distance
                  END AS wh_per_km
                FROM total_range_loss, distance
                LEFT JOIN cars ON cars.id = :carId
                """.formatted(rangeMode);
        return jdbc.query(sql, carTime(carId, from, to), rs -> rs.next() ? getDouble(rs, "wh_per_km") : null);
    }

    public Double totalDistance(long carId, Instant from, Instant to) {
        String sql = """
                SELECT coalesce(sum(distance), 0)
                FROM drives
                WHERE car_id = :carId AND start_date >= :from AND start_date <= :to
                """;
        return jdbc.queryForObject(sql, carTime(carId, from, to), Double.class);
    }

    public long driveCount(long carId, Instant from, Instant to) {
        String sql = """
                SELECT count(*) FROM drives
                WHERE car_id = :carId AND start_date >= :from AND start_date <= :to
                """;
        Long v = jdbc.queryForObject(sql, carTime(carId, from, to), Long.class);
        return v == null ? 0 : v;
    }

    public long chargeCount(long carId, Instant from, Instant to) {
        String sql = """
                SELECT count(*) FROM charging_processes
                WHERE car_id = :carId AND start_date >= :from AND start_date <= :to
                  AND (charge_energy_added IS NULL OR charge_energy_added > 0)
                """;
        Long v = jdbc.queryForObject(sql, carTime(carId, from, to), Long.class);
        return v == null ? 0 : v;
    }

    public Map<String, Double> chargeEnergyAndCost(long carId, Instant from, Instant to) {
        String sql = """
                SELECT coalesce(sum(charge_energy_added), 0) AS energy_added,
                       sum(cost) AS total_cost
                FROM charging_processes
                WHERE car_id = :carId AND start_date >= :from AND start_date <= :to
                  AND (charge_energy_added IS NULL OR charge_energy_added > 0)
                """;
        return jdbc.query(sql, carTime(carId, from, to), rs -> {
            Map<String, Double> m = new HashMap<>();
            if (rs.next()) {
                m.put("energyAdded", getDouble(rs, "energy_added"));
                m.put("cost", getDouble(rs, "total_cost"));
            }
            return m;
        });
    }

    public String latestFirmware(long carId) {
        String sql = """
                SELECT split_part(version, ' ', 1) AS version
                FROM updates WHERE car_id = :carId
                ORDER BY start_date DESC LIMIT 1
                """;
        List<String> list = jdbc.query(sql, new MapSqlParameterSource("carId", carId),
                (rs, i) -> rs.getString("version"));
        return list.isEmpty() ? null : list.getFirst();
    }

    public Boolean lfpBattery(long carId) {
        String sql = """
                SELECT cs.lfp_battery FROM cars c
                JOIN car_settings cs ON c.settings_id = cs.id
                WHERE c.id = :carId
                """;
        List<Boolean> list = jdbc.query(sql, new MapSqlParameterSource("carId", carId),
                (rs, i) -> (Boolean) rs.getObject("lfp_battery"));
        return list.isEmpty() ? null : list.getFirst();
    }

    public DriveStatsDto.Summary driveSummary(long carId, Instant from, Instant to, String rangeMode) {
        String sql = """
                SELECT
                  count(*) AS drive_count,
                  coalesce(sum(distance), 0) AS total_distance,
                  coalesce(sum(duration_min), 0) AS total_duration,
                  avg(distance) AS avg_distance,
                  percentile_cont(0.5) WITHIN GROUP (ORDER BY distance) AS median_distance,
                  max(speed_max) AS max_speed,
                  sum((start_%1$s_range_km - end_%1$s_range_km) * car.efficiency) AS total_consumption
                FROM drives d
                JOIN cars car ON car.id = d.car_id
                WHERE d.car_id = :carId AND d.start_date >= :from AND d.start_date <= :to
                  AND d.end_date IS NOT NULL
                """.formatted(rangeMode);
        return jdbc.query(sql, carTime(carId, from, to), rs -> {
            if (!rs.next()) {
                return new DriveStatsDto.Summary(0, 0, 0, null, null, null, null, null);
            }
            long count = rs.getLong("drive_count");
            double dist = rs.getDouble("total_distance");
            Double consumption = getDouble(rs, "total_consumption");
            Double avgWh = (consumption != null && dist > 0) ? (consumption * 1000.0 / dist) : null;
            return new DriveStatsDto.Summary(
                    count,
                    dist,
                    rs.getDouble("total_duration"),
                    getDouble(rs, "avg_distance"),
                    getDouble(rs, "median_distance"),
                    getInteger(rs, "max_speed"),
                    consumption,
                    avgWh
            );
        });
    }

    public List<DriveStatsDto.Bucket> driveBuckets(long carId, Instant from, Instant to, String groupBy, String rangeMode) {
        String trunc = switch (groupBy) {
            case "week" -> "week";
            case "month" -> "month";
            case "year" -> "year";
            default -> "day";
        };
        String sql = """
                SELECT
                  to_char(date_trunc('%1$s', start_date), 'YYYY-MM-DD') AS period,
                  count(*) AS drive_count,
                  coalesce(sum(distance), 0) AS distance_km,
                  coalesce(sum(duration_min), 0) AS duration_min,
                  sum((start_%2$s_range_km - end_%2$s_range_km) * car.efficiency) AS consumption_kwh,
                  CASE WHEN sum(duration_min) > 0
                       THEN sum(distance) / (sum(duration_min) * 60.0) * 3600.0
                       ELSE NULL END AS avg_speed
                FROM drives d
                JOIN cars car ON car.id = d.car_id
                WHERE d.car_id = :carId AND d.start_date >= :from AND d.start_date <= :to
                  AND d.end_date IS NOT NULL
                GROUP BY 1
                ORDER BY 1
                """.formatted(trunc, rangeMode);
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new DriveStatsDto.Bucket(
                rs.getString("period"),
                rs.getLong("drive_count"),
                rs.getDouble("distance_km"),
                rs.getDouble("duration_min"),
                getDouble(rs, "consumption_kwh"),
                getDouble(rs, "avg_speed")
        ));
    }

    public ChargingStatsDto.Summary chargingSummary(long carId, Instant from, Instant to) {
        String sql = """
                SELECT
                  count(*) AS charge_count,
                  coalesce(sum(charge_energy_added), 0) AS energy_added,
                  coalesce(sum(charge_energy_used), 0) AS energy_used,
                  sum(cost) AS total_cost,
                  coalesce(sum(duration_min), 0) AS duration_min
                FROM charging_processes
                WHERE car_id = :carId AND start_date >= :from AND start_date <= :to
                  AND (charge_energy_added IS NULL OR charge_energy_added > 0)
                  AND end_date IS NOT NULL
                """;
        return jdbc.query(sql, carTime(carId, from, to), rs -> {
            if (!rs.next()) {
                return new ChargingStatsDto.Summary(0, 0, 0, null, null, 0);
            }
            double energy = rs.getDouble("energy_added");
            Double cost = getDouble(rs, "total_cost");
            Double costPer = (cost != null && energy > 0) ? cost / energy : null;
            return new ChargingStatsDto.Summary(
                    rs.getLong("charge_count"),
                    energy,
                    rs.getDouble("energy_used"),
                    cost,
                    costPer,
                    rs.getDouble("duration_min")
            );
        });
    }

    public List<ChargingStatsDto.TypeBreakdown> chargingByType(long carId, Instant from, Instant to) {
        String sql = """
                SELECT charge_type, count(*) AS cnt,
                       coalesce(sum(energy_added), 0) AS energy_added,
                       coalesce(sum(duration_min), 0) AS duration_min,
                       sum(cost) AS cost
                FROM (
                  SELECT
                    cp.charge_energy_added AS energy_added,
                    cp.duration_min,
                    cp.cost,
                    CASE WHEN NULLIF(mode() WITHIN GROUP (ORDER BY c.charger_phases), 0) IS NULL
                         THEN 'DC' ELSE 'AC' END AS charge_type
                  FROM charging_processes cp
                  LEFT JOIN charges c ON c.charging_process_id = cp.id
                  WHERE cp.car_id = :carId AND cp.start_date >= :from AND cp.start_date <= :to
                    AND (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                    AND cp.end_date IS NOT NULL
                  GROUP BY cp.id
                ) t
                GROUP BY charge_type
                ORDER BY charge_type
                """;
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new ChargingStatsDto.TypeBreakdown(
                rs.getString("charge_type"),
                rs.getLong("cnt"),
                rs.getDouble("energy_added"),
                rs.getDouble("duration_min"),
                getDouble(rs, "cost")
        ));
    }

    public List<ChargingStatsDto.Station> topStations(long carId, Instant from, Instant to, int limit) {
        String sql = """
                SELECT
                  coalesce(g.name, CONCAT_WS(', ',
                    COALESCE(a.name, NULLIF(CONCAT_WS(' ', a.road, a.house_number), '')), a.city), 'Unknown') AS name,
                  g.id AS geofence_id,
                  count(*) AS cnt,
                  coalesce(sum(cp.charge_energy_added), 0) AS energy_added,
                  sum(cp.cost) AS cost
                FROM charging_processes cp
                LEFT JOIN geofences g ON g.id = cp.geofence_id
                LEFT JOIN addresses a ON a.id = cp.address_id
                WHERE cp.car_id = :carId AND cp.start_date >= :from AND cp.start_date <= :to
                  AND (cp.charge_energy_added IS NULL OR cp.charge_energy_added > 0)
                GROUP BY 1, 2
                ORDER BY energy_added DESC
                LIMIT :limit
                """;
        MapSqlParameterSource params = carTime(carId, from, to).addValue("limit", limit);
        return jdbc.query(sql, params, (rs, i) -> {
            long gid = rs.getLong("geofence_id");
            Long geofenceId = rs.wasNull() ? null : gid;
            return new ChargingStatsDto.Station(
                    rs.getString("name"),
                    geofenceId,
                    rs.getLong("cnt"),
                    rs.getDouble("energy_added"),
                    getDouble(rs, "cost")
            );
        });
    }

    public List<EfficiencyStatsDto.TempBucket> efficiencyByTemp(long carId, Instant from, Instant to, String rangeMode) {
        String sql = """
                SELECT
                  round(outside_temp_avg)::int AS temp_c,
                  count(*) AS drive_count,
                  coalesce(sum(distance), 0) AS distance_km,
                  CASE WHEN sum(distance) > 0
                       THEN sum((start_%1$s_range_km - end_%1$s_range_km) * car.efficiency * 1000) / sum(distance)
                       ELSE NULL END AS wh_per_km
                FROM drives d
                JOIN cars car ON car.id = d.car_id
                WHERE d.car_id = :carId AND d.start_date >= :from AND d.start_date <= :to
                  AND d.end_date IS NOT NULL AND d.outside_temp_avg IS NOT NULL
                GROUP BY 1
                ORDER BY 1
                """.formatted(rangeMode);
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new EfficiencyStatsDto.TempBucket(
                getInteger(rs, "temp_c"),
                rs.getLong("drive_count"),
                rs.getDouble("distance_km"),
                getDouble(rs, "wh_per_km")
        ));
    }

    public Double carEfficiency(long carId) {
        String sql = "SELECT efficiency FROM cars WHERE id = :carId";
        return jdbc.query(sql, new MapSqlParameterSource("carId", carId),
                rs -> rs.next() ? getDouble(rs, "efficiency") : null);
    }

    public List<PeriodStatsDto.Row> periodStats(long carId, Instant from, Instant to, String period, String rangeMode) {
        String trunc = switch (period) {
            case "week" -> "week";
            case "month" -> "month";
            case "year" -> "year";
            default -> "day";
        };
        String sql = """
                WITH drive_agg AS (
                  SELECT date_trunc('%1$s', start_date) AS bucket,
                         count(*) AS drives,
                         coalesce(sum(distance), 0) AS drive_distance,
                         sum((start_%2$s_range_km - end_%2$s_range_km) * car.efficiency) AS drive_consumption
                  FROM drives d
                  JOIN cars car ON car.id = d.car_id
                  WHERE d.car_id = :carId AND d.start_date >= :from AND d.start_date <= :to
                    AND d.end_date IS NOT NULL
                  GROUP BY 1
                ),
                charge_agg AS (
                  SELECT date_trunc('%1$s', start_date) AS bucket,
                         count(*) AS charges,
                         coalesce(sum(charge_energy_added), 0) AS energy_added,
                         sum(cost) AS charge_cost
                  FROM charging_processes
                  WHERE car_id = :carId AND start_date >= :from AND start_date <= :to
                    AND (charge_energy_added IS NULL OR charge_energy_added > 0)
                  GROUP BY 1
                )
                SELECT
                  to_char(coalesce(d.bucket, c.bucket), 'YYYY-MM-DD') AS bucket,
                  coalesce(d.drives, 0) AS drives,
                  coalesce(d.drive_distance, 0) AS drive_distance,
                  d.drive_consumption,
                  coalesce(c.charges, 0) AS charges,
                  coalesce(c.energy_added, 0) AS energy_added,
                  c.charge_cost
                FROM drive_agg d
                FULL OUTER JOIN charge_agg c ON d.bucket = c.bucket
                ORDER BY 1
                """.formatted(trunc, rangeMode);
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new PeriodStatsDto.Row(
                rs.getString("bucket"),
                rs.getLong("drives"),
                rs.getDouble("drive_distance"),
                getDouble(rs, "drive_consumption"),
                rs.getLong("charges"),
                rs.getDouble("energy_added"),
                getDouble(rs, "charge_cost")
        ));
    }

    public List<MileagePointDto> mileage(long carId, Instant from, Instant to) {
        String sql = """
                SELECT start_date AS date, start_km AS odometer
                FROM drives
                WHERE car_id = :carId AND start_date >= :from AND start_date <= :to
                  AND start_km IS NOT NULL
                UNION ALL
                SELECT end_date, end_km
                FROM drives
                WHERE car_id = :carId AND end_date >= :from AND end_date <= :to
                  AND end_km IS NOT NULL AND end_date IS NOT NULL
                ORDER BY 1
                """;
        return jdbc.query(sql, carTime(carId, from, to), (rs, i) -> new MileagePointDto(
                toInstant(rs.getTimestamp("date")),
                getDouble(rs, "odometer")
        ));
    }
}
