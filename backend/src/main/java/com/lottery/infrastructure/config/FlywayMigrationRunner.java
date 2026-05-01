package com.lottery.infrastructure.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public final class FlywayMigrationRunner {
    public void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
