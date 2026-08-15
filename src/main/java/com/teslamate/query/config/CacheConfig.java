package com.teslamate.query.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(QueryProperties props) {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                "cars", "settings", "geofences", "addresses", "carSettings",
                "currentStatus", "currentCharging", "currentDrive", "currentParking"
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(props.getCacheTtlSeconds(), TimeUnit.SECONDS));
        return manager;
    }
}
