package com.teslamate.query.dao;

import com.teslamate.query.entity.PositionEntity;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@RegisterConstructorMapper(PositionEntity.class)
public interface PositionDao {

    @SqlQuery("SELECT * FROM positions WHERE id IN (<ids>)")
    List<PositionEntity> findByIds(@BindList("ids") Collection<Long> ids);

    @SqlQuery("SELECT * FROM positions WHERE drive_id = :driveId ORDER BY date")
    List<PositionEntity> findByDriveId(@Bind("driveId") long driveId);

    @SqlQuery("SELECT * FROM positions WHERE drive_id IN (<driveIds>) ORDER BY drive_id, date")
    List<PositionEntity> findByDriveIds(@BindList("driveIds") Collection<Long> driveIds);

    @SqlQuery("""
            SELECT * FROM positions
            WHERE car_id = :carId AND date >= :from AND date <= :to
              AND ideal_battery_range_km IS NOT NULL
            ORDER BY date
            LIMIT :limit
            """)
    List<PositionEntity> findCleanForCarInRange(
            @Bind("carId") long carId,
            @Bind("from") Instant from,
            @Bind("to") Instant to,
            @Bind("limit") int limit);
}
