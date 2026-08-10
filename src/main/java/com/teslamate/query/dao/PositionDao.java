package com.teslamate.query.dao;

import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.PositionDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@RegisterConstructorMapper(PositionDto.class)
@RegisterConstructorMapper(DrivePositionDto.class)
public interface PositionDao {

    String SELECT = """
            SELECT id, car_id, drive_id, date, latitude, longitude, elevation, speed, power,
                   odometer, ideal_battery_range_km, rated_battery_range_km,
                   battery_level, usable_battery_level, outside_temp, inside_temp
            FROM positions
            """;

    @SqlQuery(SELECT + " WHERE id IN (<ids>)")
    List<PositionDto> findByIds(@BindList("ids") Collection<Long> ids);

    @SqlQuery("""
            SELECT id, date, latitude, longitude, elevation, speed, power,
                   odometer, ideal_battery_range_km, rated_battery_range_km,
                   battery_level, usable_battery_level, outside_temp, inside_temp
            FROM positions
            WHERE drive_id = :driveId
            ORDER BY date
            """)
    List<DrivePositionDto> findByDriveId(@Bind("driveId") long driveId);

    @SqlQuery(SELECT + """
            WHERE drive_id IN (<driveIds>)
            ORDER BY drive_id, date
            """)
    List<PositionDto> findByDriveIds(@BindList("driveIds") Collection<Long> driveIds);

    @SqlQuery(SELECT + """
            WHERE car_id = :carId AND date >= :from AND date <= :to
              AND ideal_battery_range_km IS NOT NULL
            ORDER BY date
            LIMIT :limit
            """)
    List<PositionDto> findCleanForCarInRange(
            @Bind("carId") long carId,
            @Bind("from") Instant from,
            @Bind("to") Instant to,
            @Bind("limit") int limit);
}
