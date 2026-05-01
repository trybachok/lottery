package com.lottery.infrastructure.config;

import java.util.Map;

public record ApplicationProperties(
        int httpPort,
        String jdbcUrl,
        String jdbcUser,
        String jdbcPassword,
        boolean migrationsEnabled,
        int bcryptCost) {
    public static ApplicationProperties fromEnvironment(Map<String, String> env) {
        return new ApplicationProperties(
                intValue(env, "LOTTERY_HTTP_PORT", 8080),
                env.getOrDefault("LOTTERY_JDBC_URL", "jdbc:postgresql://localhost:5432/lottery"),
                env.getOrDefault("LOTTERY_JDBC_USER", "lottery"),
                env.getOrDefault("LOTTERY_JDBC_PASSWORD", "lottery"),
                booleanValue(env, "LOTTERY_DB_MIGRATIONS_ENABLED", false),
                intValue(env, "LOTTERY_BCRYPT_COST", 12));
    }

    private static int intValue(Map<String, String> env, String key, int defaultValue) {
        String value = env.get(key);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    private static boolean booleanValue(Map<String, String> env, String key, boolean defaultValue) {
        String value = env.get(key);
        return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value);
    }
}
