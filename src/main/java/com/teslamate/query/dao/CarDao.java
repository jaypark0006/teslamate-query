package com.teslamate.query.dao;

import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(CarDto.class)
@RegisterConstructorMapper(LatestSnapshotDto.class)
public interface CarDao {

    @SqlQuery("""
            SELECT c.id, c.name, c.vin, c.model, c.marketing_name, c.trim_badging,
                   c.efficiency, c.display_priority, c.exterior_color, c.wheel_type,
                   cs.lfp_battery, cs.free_supercharging, cs.enabled
            FROM cars c
            LEFT JOIN car_settings cs ON c.settings_id = cs.id
            ORDER BY c.display_priority NULLS LAST, c.name NULLS LAST, c.vin
            """)
    List<CarDto> findAll();

    @SqlQuery("""
            SELECT c.id, c.name, c.vin, c.model, c.marketing_name, c.trim_badging,
                   c.efficiency, c.display_priority, c.exterior_color, c.wheel_type,
                   cs.lfp_battery, cs.free_supercharging, cs.enabled
            FROM cars c
            LEFT JOIN car_settings cs ON c.settings_id = cs.id
            WHERE c.id = :id
            """)
    Optional<CarDto> findById(@Bind("id") long id);

    @SqlQuery("""
            SELECT * FROM (
              (SELECT p.date, 'position' AS source, p.battery_level, p.usable_battery_level,
                      p.ideal_battery_range_km, p.rated_battery_range_km, p.odometer AS odometer_km,
                      p.latitude, p.longitude, p.outside_temp AS outside_temp_c, p.inside_temp AS inside_temp_c,
                      p.speed, p.power, NULL::int AS charger_power, NULL::int AS charger_voltage,
                      :carId AS car_id
               FROM positions p
               WHERE p.car_id = :carId AND p.ideal_battery_range_km IS NOT NULL
               ORDER BY p.date DESC
               LIMIT 1)
              UNION ALL
              (SELECT c.date, 'charge' AS source, c.battery_level, c.usable_battery_level,
                      c.ideal_battery_range_km, c.rated_battery_range_km, pos.odometer AS odometer_km,
                      pos.latitude, pos.longitude, c.outside_temp AS outside_temp_c, NULL::numeric AS inside_temp_c,
                      NULL::int AS speed, NULL::int AS power, c.charger_power, c.charger_voltage,
                      :carId AS car_id
               FROM charges c
               JOIN charging_processes cp ON cp.id = c.charging_process_id
               LEFT JOIN positions pos ON pos.id = cp.position_id
               WHERE cp.car_id = :carId
               ORDER BY c.date DESC
               LIMIT 1)
            ) t
            ORDER BY date DESC
            LIMIT 1
            """)
    Optional<LatestSnapshotDto> findLatest(@Bind("carId") long carId);
}
