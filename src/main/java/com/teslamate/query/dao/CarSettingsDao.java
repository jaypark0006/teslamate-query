package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.entity.CarSettingsEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** car_settings table. */
@Repository
public class CarSettingsDao {

    private final Jdbi jdbi;

    public CarSettingsDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<CarSettingsEntity> findAll() {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM car_settings
                ORDER BY id
                """)
                .map(ConstructorMapper.of(CarSettingsEntity.class))
                .list());
    }

    public Optional<CarSettingsEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM car_settings
                WHERE id = :id
                """)
                .bind("id", id)
                .map(ConstructorMapper.of(CarSettingsEntity.class))
                .findOne());
    }

    public List<CarSettingsEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM car_settings
                WHERE id IN (<ids>)
                """)
                .bindList("ids", ids)
                .map(ConstructorMapper.of(CarSettingsEntity.class))
                .list());
    }
}
