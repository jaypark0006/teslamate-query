package com.teslamate.query.dao;

import com.teslamate.query.dto.ChargeSampleDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

@RegisterConstructorMapper(ChargeSampleDto.class)
public interface ChargeDao {

    @SqlQuery("""
            SELECT id, date, battery_level, usable_battery_level, charge_energy_added,
                   charger_power, charger_voltage, charger_actual_current, charger_phases,
                   fast_charger_present, fast_charger_type,
                   ideal_battery_range_km, rated_battery_range_km, outside_temp, battery_heater_on
            FROM charges
            WHERE charging_process_id = :id
            ORDER BY date
            """)
    List<ChargeSampleDto> findByProcessId(@Bind("id") long processId);
}
