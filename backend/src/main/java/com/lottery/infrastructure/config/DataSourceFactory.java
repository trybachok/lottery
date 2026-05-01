package com.lottery.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public final class DataSourceFactory {
    public DataSource create(ApplicationProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.jdbcUrl());
        config.setUsername(properties.jdbcUser());
        config.setPassword(properties.jdbcPassword());
        config.setMaximumPoolSize(10);
        config.setPoolName("lottery-postgres-pool");
        return new HikariDataSource(config);
    }
}
