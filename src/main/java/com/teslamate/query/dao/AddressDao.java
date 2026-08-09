package com.teslamate.query.dao;

import com.teslamate.query.dto.AddressDto;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(AddressDto.class)
public interface AddressDao {

    @SqlQuery("""
            SELECT id, display_name, name, road, house_number, neighbourhood, city, county,
                   postcode, state, state_district, country, latitude, longitude, osm_id, osm_type
            FROM addresses
            WHERE id = :id
            """)
    Optional<AddressDto> findById(@Bind("id") long id);

    @SqlQuery("""
            SELECT id, display_name, name, road, house_number, neighbourhood, city, county,
                   postcode, state, state_district, country, latitude, longitude, osm_id, osm_type
            FROM addresses
            WHERE id IN (<ids>)
            """)
    List<AddressDto> findByIds(@BindList("ids") Collection<Long> ids);
}
