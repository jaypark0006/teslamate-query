package com.teslamate.query.dao;

import com.teslamate.query.dto.UpdateDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.time.Instant;
import java.util.List;

@RegisterConstructorMapper(UpdateDto.class)
public interface UpdateDao {

    @SqlQuery("""
            SELECT id, car_id, start_date, end_date, version
            FROM updates
            WHERE car_id = :carId
              AND start_date >= :from AND start_date <= :to
            ORDER BY start_date DESC
            """)
    List<UpdateDto> findByCarAndTime(
            @Bind("carId") long carId,
            @Bind("from") Instant from,
            @Bind("to") Instant to);

    @SqlQuery("""
            SELECT split_part(version, ' ', 1)
            FROM updates WHERE car_id = :carId
            ORDER BY start_date DESC LIMIT 1
            """)
    String latestFirmware(@Bind("carId") long carId);
}
