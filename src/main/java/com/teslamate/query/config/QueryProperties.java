package com.teslamate.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "teslamate.query")
public class QueryProperties {

    private String apiKeys = "dev-api-key";
    private boolean authEnabled = false;
    private int defaultPageSize = 50;
    private int maxPageSize = 500;
    private int cacheTtlSeconds = 60;

    public List<String> resolvedApiKeys() {
        return Arrays.stream(apiKeys.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public String getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(String apiKeys) {
        this.apiKeys = apiKeys;
    }

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }
}
