package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.DriveSearchCondition;
import com.teslamate.query.dto.DriveDto;
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

/**
 * Drive access: fixed SQL + ASRS-style {@code whereClause}/{@code @BindMap} for filters.
 */
@RegisterConstructorMapper(DriveDto.class)
public interface DriveDao {

    String SELECT = """
            SELECT
              d.id, d.car_id, d.start_date, d.end_date, d.duration_min,
              d.distance AS distance_km,
              d.start_ideal_range_km, d.end_ideal_range_km,
              d.start_rated_range_km, d.end_rated_range_km,
              d.outside_temp_avg AS outside_temp_avg_c,
              d.inside_temp_avg AS inside_temp_avg_c,
              CASE WHEN d.duration_min > 0
                   THEN d.distance / (d.duration_min * 60.0) * 3600.0
                   ELSE NULL END AS avg_speed_kmh,
              d.speed_max, d.power_max, d.power_min, d.ascent, d.descent,
              d.start_position_id, d.end_position_id,
              d.start_address_id, d.end_address_id,
              d.start_geofence_id, d.end_geofence_id
            FROM drives d
            """;

    @SqlQuery("SELECT COUNT(*) FROM drives d <whereClause>")
    @UseStringTemplateEngine
    long count(@Define("whereClause") String whereClause, @BindMap Map<String, Object> params);

    @SqlQuery("""
            SELECT d.id FROM drives d
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

    @SqlQuery(SELECT + " WHERE d.id IN (<ids>)")
    List<DriveDto> findByIds(@BindList("ids") Collection<Long> ids);

    @SqlQuery(SELECT + " WHERE d.id = :id")
    Optional<DriveDto> findById(@Bind("id") long id);

    default long count(DriveSearchCondition condition) {
        return count(condition.whereClause(), condition.params());
    }

    default List<Long> findIds(DriveSearchCondition condition, int limit, int offset) {
        return findIds(condition.whereClause(), condition.sortClause(), condition.params(), limit, offset);
    }

    default List<DriveDto> findByIdsOrdered(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return IdOrder.align(ids, findByIds(ids), DriveDto::id);
    }
}
