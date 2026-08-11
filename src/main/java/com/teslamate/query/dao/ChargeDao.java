package com.teslamate.query.dao;

import com.teslamate.query.entity.ChargeEntity;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

@RegisterConstructorMapper(ChargeEntity.class)
public interface ChargeDao {

    @SqlQuery("SELECT * FROM charges WHERE charging_process_id = :id ORDER BY date")
    List<ChargeEntity> findByProcessId(@Bind("id") long processId);
}
