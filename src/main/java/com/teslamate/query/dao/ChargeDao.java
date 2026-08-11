package com.teslamate.query.dao;

import com.teslamate.query.entity.ChargeEntity;
import com.teslamate.query.entity.ChargeEntity.Table;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

@RegisterConstructorMapper(ChargeEntity.class)
public interface ChargeDao {

    @SqlQuery("SELECT " + Table.COLUMNS + " FROM " + Table.NAME
            + " WHERE " + Table.CHARGING_PROCESS_ID + " = :id ORDER BY " + Table.DATE)
    List<ChargeEntity> findByProcessId(@Bind("id") long processId);
}
