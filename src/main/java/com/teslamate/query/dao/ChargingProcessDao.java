package com.teslamate.query.dao;

import com.teslamate.query.db.IdOrder;
import com.teslamate.query.db.condition.ChargingProcessSearchCondition;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.ChargingProcessEntity.Table;
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

@RegisterConstructorMapper(ChargingProcessEntity.class)
public interface ChargingProcessDao {

    @SqlQuery("SELECT COUNT(*) FROM " + Table.NAME + " <whereClause>")
    @UseStringTemplateEngine
    long count(@Define("whereClause") String whereClause, @BindMap Map<String, Object> params);

    @SqlQuery("SELECT " + Table.ID + " FROM " + Table.NAME + " <whereClause> <sortClause> LIMIT :limit OFFSET :offset")
    @UseStringTemplateEngine
    List<Long> findIds(
            @Define("whereClause") String whereClause,
            @Define("sortClause") String sortClause,
            @BindMap Map<String, Object> params,
            @Bind("limit") int limit,
            @Bind("offset") int offset);

    @SqlQuery("SELECT " + Table.COLUMNS + " FROM " + Table.NAME + " WHERE " + Table.ID + " IN (<ids>)")
    List<ChargingProcessEntity> findByIds(@BindList("ids") Collection<Long> ids);

    @SqlQuery("SELECT " + Table.COLUMNS + " FROM " + Table.NAME + " WHERE " + Table.ID + " = :id")
    Optional<ChargingProcessEntity> findById(@Bind("id") long id);

    default long count(ChargingProcessSearchCondition condition) {
        return count(condition.whereClause(), condition.params());
    }

    default List<Long> findIds(ChargingProcessSearchCondition condition, int limit, int offset) {
        return findIds(condition.whereClause(), condition.sortClause(), condition.params(), limit, offset);
    }

    default List<ChargingProcessEntity> findByIdsOrdered(Collection<Long> ids) {
        if (IdOrder.isEmpty(ids)) {
            return List.of();
        }
        return IdOrder.align(ids, findByIds(ids), ChargingProcessEntity::id);
    }
}
