package com.teslamate.query.config;

import com.teslamate.query.dao.*;
import com.teslamate.query.dto.*;
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

        // Analytics/stats fluent mapTo types
        for (Class<?> type : List.of(
                ChargeEnergyCostDto.class,
                DriveStatsDto.Summary.class, DriveStatsDto.Bucket.class,
                ChargingStatsDto.Summary.class, ChargingStatsDto.TypeBreakdown.class, ChargingStatsDto.Station.class,
                EfficiencyStatsDto.TempBucket.class, PeriodStatsDto.Row.class, MileagePointDto.class,
                StateDto.class, UpdateDto.class, TimelineEventDto.class,
                VampireDrainDto.Segment.class, ProjectedRangeDto.Point.class,
                BatteryHealthDto.CapacityPoint.class, LocationStatsDto.Place.class, PositionDto.class
        )) {
            jdbi.registerRowMapper(type, ConstructorMapper.of(type));
        }
        return jdbi;
    }

    @Bean public HealthDao healthDao(Jdbi jdbi) { return jdbi.onDemand(HealthDao.class); }
    @Bean public CarDao carDao(Jdbi jdbi) { return jdbi.onDemand(CarDao.class); }
    @Bean public SettingsDao settingsDao(Jdbi jdbi) { return jdbi.onDemand(SettingsDao.class); }
    @Bean public GeofenceDao geofenceDao(Jdbi jdbi) { return jdbi.onDemand(GeofenceDao.class); }
    @Bean public AddressDao addressDao(Jdbi jdbi) { return jdbi.onDemand(AddressDao.class); }
    @Bean public DriveDao driveDao(Jdbi jdbi) { return jdbi.onDemand(DriveDao.class); }
    @Bean public ChargingProcessDao chargingProcessDao(Jdbi jdbi) { return jdbi.onDemand(ChargingProcessDao.class); }
    @Bean public ChargeDao chargeDao(Jdbi jdbi) { return jdbi.onDemand(ChargeDao.class); }
    @Bean public PositionDao positionDao(Jdbi jdbi) { return jdbi.onDemand(PositionDao.class); }
    @Bean public StateDao stateDao(Jdbi jdbi) { return jdbi.onDemand(StateDao.class); }
    @Bean public UpdateDao updateDao(Jdbi jdbi) { return jdbi.onDemand(UpdateDao.class); }
}
