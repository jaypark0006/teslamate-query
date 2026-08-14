package com.teslamate.query.dao;

import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface HealthDao {
    @SqlQuery("SELECT 1")
    int ping();
}
