package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.entity.CarEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** cars table — single-table reads. Latest snapshot is multi-table (composition endpoint). */
@Repository
public class CarDao {

    private final Jdbi jdbi;

    public CarDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<CarEntity> findAll() {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT * FROM cars ORDER BY display_priority NULLS LAST, name NULLS LAST, vin")
                .map(ConstructorMapper.of(CarEntity.class))
                .list());
    }

    public Optional<CarEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM cars WHERE id = :id")
                .bind("id", id)
                .map(ConstructorMapper.of(CarEntity.class))
                .findOne());
    }

    public List<CarEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("SELECT * FROM cars WHERE id IN (<ids>)")
                .bindList("ids", ids)
                .map(ConstructorMapper.of(CarEntity.class))
                .list());
    }

    /**
     * Multi-table composition for {@code GET /cars/{id}/latest} only.
     * Not a single-table Condition query.
     */
    public Optional<LatestSnapshotDto> findLatest(long carId) {
        return jdbi.withHandle(h -> h.createQuery("""
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
                .bind("carId", carId)
                .map(ConstructorMapper.of(LatestSnapshotDto.class))
                .findOne());
    }
}
