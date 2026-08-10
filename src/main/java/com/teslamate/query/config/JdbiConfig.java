package com.teslamate.query.config;

import com.teslamate.query.dao.AddressDao;
import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.ChargingProcessDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.GeofenceDao;
import com.teslamate.query.dao.HealthDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.dao.SettingsDao;
import com.teslamate.query.dto.BatteryHealthDto;
import com.teslamate.query.dto.ChargeEnergyCostDto;
import com.teslamate.query.dto.ChargingProcessEnrichedDto;
import com.teslamate.query.dto.ChargingStatsDto;
import com.teslamate.query.dto.DriveEnrichedDto;
import com.teslamate.query.dto.DriveStatsDto;
import com.teslamate.query.dto.EfficiencyStatsDto;
import com.teslamate.query.dto.LocationStatsDto;
import com.teslamate.query.dto.MileagePointDto;
import com.teslamate.query.dto.PeriodStatsDto;
import com.teslamate.query.dto.ProjectedRangeDto;
import com.teslamate.query.dto.StateDto;
import com.teslamate.query.dto.TimelineEventDto;
import com.teslamate.query.dto.UpdateDto;
import com.teslamate.query.dto.VampireDrainDto;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.jdbi.v3.core.mapper.reflect.SnakeCaseColumnNameMatcher;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.stringtemplate4.StringTemplateEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

/**
 * Spring DI for JDBI (ASRS-style: SqlObject + optional whereClause via StringTemplate).
 * Dao interfaces carry {@code @RegisterConstructorMapper}; only analytics DTOs need global register.
 */
@Configuration
public class JdbiConfig {

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new PostgresPlugin());
        jdbi.setTemplateEngine(new StringTemplateEngine());
        jdbi.getConfig(ReflectionMappers.class)
                .setColumnNameMatchers(List.of(new SnakeCaseColumnNameMatcher()));

        // Fluent/analytics queries (not on SqlObject interfaces)
        register(jdbi, ChargeEnergyCostDto.class);
        register(jdbi, DriveEnrichedDto.class);
        register(jdbi, ChargingProcessEnrichedDto.class);
        register(jdbi, DriveStatsDto.Summary.class);
        register(jdbi, DriveStatsDto.Bucket.class);
        register(jdbi, ChargingStatsDto.Summary.class);
        register(jdbi, ChargingStatsDto.TypeBreakdown.class);
        register(jdbi, ChargingStatsDto.Station.class);
        register(jdbi, EfficiencyStatsDto.TempBucket.class);
        register(jdbi, PeriodStatsDto.Row.class);
        register(jdbi, MileagePointDto.class);
        register(jdbi, StateDto.class);
        register(jdbi, UpdateDto.class);
        register(jdbi, TimelineEventDto.class);
        register(jdbi, VampireDrainDto.Segment.class);
        register(jdbi, ProjectedRangeDto.Point.class);
        register(jdbi, BatteryHealthDto.CapacityPoint.class);
        register(jdbi, LocationStatsDto.Place.class);
        return jdbi;
    }

    private static void register(Jdbi jdbi, Class<?> type) {
        jdbi.registerRowMapper(type, ConstructorMapper.of(type));
    }

    @Bean
    public HealthDao healthDao(Jdbi jdbi) {
        return jdbi.onDemand(HealthDao.class);
    }

    @Bean
    public CarDao carDao(Jdbi jdbi) {
        return jdbi.onDemand(CarDao.class);
    }

    @Bean
    public SettingsDao settingsDao(Jdbi jdbi) {
        return jdbi.onDemand(SettingsDao.class);
    }

    @Bean
    public GeofenceDao geofenceDao(Jdbi jdbi) {
        return jdbi.onDemand(GeofenceDao.class);
    }

    @Bean
    public AddressDao addressDao(Jdbi jdbi) {
        return jdbi.onDemand(AddressDao.class);
    }

    @Bean
    public DriveDao driveDao(Jdbi jdbi) {
        return jdbi.onDemand(DriveDao.class);
    }

    @Bean
    public ChargingProcessDao chargingProcessDao(Jdbi jdbi) {
        return jdbi.onDemand(ChargingProcessDao.class);
    }

    @Bean
    public ChargeDao chargeDao(Jdbi jdbi) {
        return jdbi.onDemand(ChargeDao.class);
    }

    @Bean
    public PositionDao positionDao(Jdbi jdbi) {
        return jdbi.onDemand(PositionDao.class);
    }
}
