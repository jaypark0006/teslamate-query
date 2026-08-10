package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.dto.ChargingProcessDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindMap;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.stringtemplate4.UseStringTemplateEngine;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RegisterConstructorMapper(ChargingProcessDto.class)
public interface ChargingProcessDao {

    String SELECT = """
            SELECT
              cp.id, cp.car_id, cp.start_date, cp.end_date,
              cp.charge_energy_added, cp.charge_energy_used, cp.duration_min,
              cp.start_battery_level, cp.end_battery_level,
              cp.start_ideal_range_km, cp.end_ideal_range_km,
              cp.start_rated_range_km, cp.end_rated_range_km,
              cp.outside_temp_avg AS outside_temp_avg_c, cp.cost,
              cp.position_id, cp.address_id, cp.geofence_id
            FROM charging_processes cp
            """;

    @SqlQuery("SELECT COUNT(*) FROM charging_processes cp <whereClause>")
    @UseStringTemplateEngine
    long count(@Define("whereClause") String whereClause, @BindMap Map<String, Object> params);

    @SqlQuery("""
            SELECT cp.id FROM charging_processes cp
            <whereClause>
            <sortClause>
            LIMIT :limit OFFSET :offset
            """)
    @UseStringTemplateEngine
    List<Long> findIds(
            @Define("whereClause") String whereClause,
            @Define("sortClause") String sortClause,
            @BindMap Map<String, Object> params,
            @Bind("limit") int limit,
            @Bind("offset") int offset);

    @SqlQuery(SELECT + " WHERE cp.id IN (<ids>)")
    List<ChargingProcessDto> findByIds(@BindList("ids") Collection<Long> ids);

    @SqlQuery(SELECT + " WHERE cp.id = :id")
    Optional<ChargingProcessDto> findById(@Bind("id") long id);

    default long count(ChargingProcessSearchCondition condition) {
        return count(condition.whereClause(), condition.params());
    }

    default List<Long> findIds(ChargingProcessSearchCondition condition, int limit, int offset) {
        return findIds(condition.whereClause(), condition.sortClause(), condition.params(), limit, offset);
    }

    default List<ChargingProcessDto> findByIdsOrdered(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return IdOrder.align(ids, findByIds(ids), ChargingProcessDto::id);
    }
}
