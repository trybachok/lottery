package com.lottery.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationPropertiesTest {
    @Test
    void usesRenderPortAndDatabaseUrlWhenLotteryOverridesAreAbsent() {
        ApplicationProperties properties = ApplicationProperties.fromEnvironment(Map.of(
                "PORT", "10000",
                "DATABASE_URL", "postgresql://lottery_user:p%40ss@postgres.internal:5432/lottery_db"));

        assertEquals(10000, properties.httpPort());
        assertEquals("jdbc:postgresql://postgres.internal:5432/lottery_db", properties.jdbcUrl());
        assertEquals("lottery_user", properties.jdbcUser());
        assertEquals("p@ss", properties.jdbcPassword());
    }

    @Test
    void lotteryEnvironmentOverridesRenderDefaults() {
        ApplicationProperties properties = ApplicationProperties.fromEnvironment(Map.of(
                "PORT", "10000",
                "LOTTERY_HTTP_PORT", "8080",
                "DATABASE_URL", "postgresql://render_user:render_pass@postgres.internal:5432/render_db",
                "LOTTERY_JDBC_URL", "jdbc:postgresql://postgres:5432/lottery",
                "LOTTERY_JDBC_USER", "lottery",
                "LOTTERY_JDBC_PASSWORD", "secret"));

        assertEquals(8080, properties.httpPort());
        assertEquals("jdbc:postgresql://postgres:5432/lottery", properties.jdbcUrl());
        assertEquals("lottery", properties.jdbcUser());
        assertEquals("secret", properties.jdbcPassword());
    }
}
