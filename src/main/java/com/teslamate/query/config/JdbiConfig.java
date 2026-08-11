package com.teslamate.query.config;

import com.teslamate.query.dao.AddressDao;
import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.ChargeDao;
import com.teslamate.query.dao.GeofenceDao;
import com.teslamate.query.dao.HealthDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.dao.SettingsDao;
import com.teslamate.query.dao.StateDao;
import com.teslamate.query.dao.UpdateDao;
import com.teslamate.query.dto.BatteryHealthDto;
import com.teslamate.query.dto.ChargeEnergyCostDto;
import com.teslamate.query.dto.ChargingStatsDto;
import com.teslamate.query.dto.DriveStatsDto;
import com.teslamate.query.dto.EfficiencyStatsDto;
import com.teslamate.query.dto.LocationStatsDto;
import com.teslamate.query.dto.MileagePointDto;
import com.teslamate.query.dto.PeriodStatsDto;
import com.teslamate.query.dto.PositionDto;
import com.teslamate.query.dto.ProjectedRangeDto;
import com.teslamate.query.dto.StateDto;
import com.teslamate.query.dto.TimelineEventDto;
import com.teslamate.query.dto.UpdateDto;
import com.teslamate.query.dto.VampireDrainDto;
import com.teslamate.query.entity.ChargingProcessEntity;
import com.teslamate.query.entity.DriveEntity;
import com.teslamate.query.entity.PositionEntity;
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

/**
 * JDBI under Spring DI. No StringTemplate engine — dynamic WHERE is plain string concat
 * so SQL {@code <}/{@code >} never need template escaping.
 */
@Configuration
public class JdbiConfig {

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new PostgresPlugin());
        jdbi.getConfig(ReflectionMappers.class)
                .setColumnNameMatchers(List.of(new SnakeCaseColumnNameMatcher()));

        for (Class<?> type : List.of(
                DriveEntity.class, ChargingProcessEntity.class, PositionEntity.class,
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
    @Bean public ChargeDao chargeDao(Jdbi jdbi) { return jdbi.onDemand(ChargeDao.class); }
    @Bean public PositionDao positionDao(Jdbi jdbi) { return jdbi.onDemand(PositionDao.class); }
    @Bean public StateDao stateDao(Jdbi jdbi) { return jdbi.onDemand(StateDao.class); }
    @Bean public UpdateDao updateDao(Jdbi jdbi) { return jdbi.onDemand(UpdateDao.class); }
    // DriveDao + ChargingProcessDao are @Repository classes (inject Jdbi)
}
