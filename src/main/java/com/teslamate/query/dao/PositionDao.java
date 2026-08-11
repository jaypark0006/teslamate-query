package com.teslamate.query.dao;

import com.teslamate.query.entity.PositionEntity;
import com.teslamate.query.entity.PositionEntity.Table;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@RegisterConstructorMapper(PositionEntity.class)
public interface PositionDao {

    @SqlQuery("SELECT " + Table.COLUMNS + " FROM " + Table.NAME + " WHERE " + Table.ID + " IN (<ids>)")
    List<PositionEntity> findByIds(@BindList("ids") Collection<Long> ids);

    @SqlQuery("SELECT " + Table.COLUMNS + " FROM " + Table.NAME
            + " WHERE " + Table.DRIVE_ID + " = :driveId ORDER BY " + Table.DATE)
    List<PositionEntity> findByDriveId(@Bind("driveId") long driveId);

    @SqlQuery("SELECT " + Table.COLUMNS + " FROM " + Table.NAME
            + " WHERE " + Table.DRIVE_ID + " IN (<driveIds>) ORDER BY " + Table.DRIVE_ID + ", " + Table.DATE)
    List<PositionEntity> findByDriveIds(@BindList("driveIds") Collection<Long> driveIds);

    @SqlQuery("SELECT " + Table.COLUMNS + " FROM " + Table.NAME
            + " WHERE " + Table.CAR_ID + " = :carId AND " + Table.DATE + " >= :from AND " + Table.DATE + " <= :to"
            + " AND " + Table.IDEAL_BATTERY_RANGE_KM + " IS NOT NULL"
            + " ORDER BY " + Table.DATE + " LIMIT :limit")
    List<PositionEntity> findCleanForCarInRange(
            @Bind("carId") long carId,
            @Bind("from") Instant from,
            @Bind("to") Instant to,
            @Bind("limit") int limit);
}
