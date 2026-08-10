package com.teslamate.query.dao;

import com.teslamate.query.dto.ChargeSampleDto;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChargeDao {

    private final Jdbi jdbi;

    public ChargeDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<ChargeSampleDto> findByProcessId(long processId) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT id, date, battery_level, usable_battery_level, charge_energy_added,
                               charger_power, charger_voltage, charger_actual_current, charger_phases,
                               fast_charger_present, fast_charger_type,
                               ideal_battery_range_km, rated_battery_range_km, outside_temp, battery_heater_on
                        FROM charges
                        WHERE charging_process_id = :id
                        ORDER BY date
                        """)
                .bind("id", processId)
                .mapTo(ChargeSampleDto.class)
                .list());
    }
}
