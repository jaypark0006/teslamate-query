package com.teslamate.query.config;

import com.teslamate.query.dao.HealthDao;
import org.jdbi.v3.core.Jdbi;
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
        jdbi.getConfig(ReflectionMappers.class)
                .setColumnNameMatchers(List.of(new SnakeCaseColumnNameMatcher()));
        return jdbi;
    }

    /** Only SqlObject interfaces need onDemand beans; @Repository Daos are Spring-managed. */
    @Bean
    public HealthDao healthDao(Jdbi jdbi) {
        return jdbi.onDemand(HealthDao.class);
    }
}
