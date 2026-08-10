package com.teslamate.query.dao;

import com.teslamate.query.dto.StateDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.time.Instant;
import java.util.List;

@RegisterConstructorMapper(StateDto.class)
public interface StateDao {

    @SqlQuery("""
            SELECT id, car_id, state::text AS state, start_date, end_date,
                   CASE WHEN end_date IS NOT NULL
                        THEN extract(epoch FROM (end_date - start_date))::bigint
                        ELSE NULL END AS duration_seconds
            FROM states
            WHERE car_id = :carId
              AND start_date <= :to
              AND (end_date IS NULL OR end_date >= :from)
            ORDER BY start_date
            """)
    List<StateDto> findByCarAndTime(
            @Bind("carId") long carId,
            @Bind("from") Instant from,
            @Bind("to") Instant to);
}
