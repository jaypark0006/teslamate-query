package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.entity.CarEntity;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class CarDao {

    private final Jdbi jdbi;

    public CarDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<CarEntity> findAll() {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM cars
                ORDER BY display_priority NULLS LAST, name NULLS LAST, vin
                """)
                .map(ConstructorMapper.of(CarEntity.class))
                .list());
    }

    public Optional<CarEntity> findById(long id) {
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM cars
                WHERE id = :id
                """)
                .bind("id", id)
                .map(ConstructorMapper.of(CarEntity.class))
                .findOne());
    }

    public Optional<CarEntity> findByVin(String vin) {
        if (vin == null || vin.isBlank()) {
            return Optional.empty();
        }
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM cars
                WHERE vin = :vin
                """)
                .bind("vin", vin.trim())
                .map(ConstructorMapper.of(CarEntity.class))
                .findOne());
    }

    public List<CarEntity> findByIds(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return jdbi.withHandle(h -> h.createQuery("""
                SELECT * FROM cars
                WHERE id IN (<ids>)
                """)
                .bindList("ids", ids)
                .map(ConstructorMapper.of(CarEntity.class))
                .list());
    }
}
