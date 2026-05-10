package com.lottery.infrastructure.config;

import java.util.Map;

public record ApplicationProperties(
        int httpPort,
        String jdbcUrl,
        String jdbcUser,
        String jdbcPassword,
        boolean migrationsEnabled,
        int bcryptCost,
        long accessTokenTtlSeconds,
        String accessTokenSecret,
        String corsAllowedOrigins,
        String frontendBaseUrl,
        String backendBaseUrl,
        String mockPaymentWebhookSecret) {
    public static ApplicationProperties fromEnvironment(Map<String, String> env) {
        return new ApplicationProperties(
                intValue(env, "LOTTERY_HTTP_PORT", 8080),
                env.getOrDefault("LOTTERY_JDBC_URL", "jdbc:postgresql://localhost:5432/lottery"),
                env.getOrDefault("LOTTERY_JDBC_USER", "lottery"),
                env.getOrDefault("LOTTERY_JDBC_PASSWORD", "lottery"),
                booleanValue(env, "LOTTERY_DB_MIGRATIONS_ENABLED", false),
                intValue(env, "LOTTERY_BCRYPT_COST", 12),
                intValue(env, "LOTTERY_ACCESS_TOKEN_TTL_SECONDS", 900),
                env.getOrDefault("LOTTERY_ACCESS_TOKEN_SECRET", "local-dev-token-secret-change-me-32-bytes"),
                env.getOrDefault("LOTTERY_CORS_ALLOWED_ORIGINS", "http://localhost:5173,http://localhost:8080"),
                env.getOrDefault("LOTTERY_FRONTEND_BASE_URL", "http://localhost:5173"),
                env.getOrDefault("LOTTERY_BACKEND_BASE_URL", "http://localhost:8080"),
                env.getOrDefault("LOTTERY_MOCK_PAYMENT_WEBHOOK_SECRET", "local-mock-payment-secret"));
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
