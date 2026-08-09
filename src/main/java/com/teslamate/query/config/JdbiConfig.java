package com.teslamate.query.config;

import com.teslamate.query.dao.AddressDao;
import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.GeofenceDao;
import com.teslamate.query.dao.HealthDao;
import com.teslamate.query.dao.SettingsDao;
import com.teslamate.query.dto.AddressDto;
import com.teslamate.query.dto.BatteryHealthDto;
import com.teslamate.query.dto.CarDto;
import com.teslamate.query.dto.ChargeEnergyCostDto;
import com.teslamate.query.dto.ChargeSampleDto;
import com.teslamate.query.dto.ChargingProcessDto;
import com.teslamate.query.dto.ChargingStatsDto;
import com.teslamate.query.dto.DriveDto;
import com.teslamate.query.dto.DrivePositionDto;
import com.teslamate.query.dto.DriveStatsDto;
import com.teslamate.query.dto.EfficiencyStatsDto;
import com.teslamate.query.dto.GeofenceDto;
import com.teslamate.query.dto.LatestSnapshotDto;
import com.teslamate.query.dto.LocationStatsDto;
import com.teslamate.query.dto.MileagePointDto;
import com.teslamate.query.dto.PeriodStatsDto;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.dto.ProjectedRangeDto;
import com.teslamate.query.dto.SettingsDto;
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
        // camelCase Java fields <-> snake_case SQL aliases
        jdbi.getConfig(ReflectionMappers.class)
                .setColumnNameMatchers(List.of(new SnakeCaseColumnNameMatcher()));

        register(jdbi, CarDto.class);
        register(jdbi, AddressDto.class);
        register(jdbi, ChargeEnergyCostDto.class);
        register(jdbi, SettingsDto.class);
        register(jdbi, GeofenceDto.class);
        register(jdbi, LatestSnapshotDto.class);
        register(jdbi, DriveDto.class);
        register(jdbi, DrivePositionDto.class);
        register(jdbi, ChargingProcessDto.class);
        register(jdbi, ChargeSampleDto.class);
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
        register(jdbi, PositionDto.class);
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
}
