package com.lottery.infrastructure.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
        DatabaseConnection databaseConnection = databaseConnection(env);
        return new ApplicationProperties(
                httpPort(env),
                databaseConnection.jdbcUrl(),
                databaseConnection.user(),
                databaseConnection.password(),
                booleanValue(env, "LOTTERY_DB_MIGRATIONS_ENABLED", false),
                intValue(env, "LOTTERY_BCRYPT_COST", 12),
                intValue(env, "LOTTERY_ACCESS_TOKEN_TTL_SECONDS", 900),
                env.getOrDefault("LOTTERY_ACCESS_TOKEN_SECRET", "local-dev-token-secret-change-me-32-bytes"),
                env.getOrDefault("LOTTERY_CORS_ALLOWED_ORIGINS", "http://localhost:5173,http://localhost:8080"),
                env.getOrDefault("LOTTERY_FRONTEND_BASE_URL", "http://localhost:5173"),
                env.getOrDefault("LOTTERY_BACKEND_BASE_URL", "http://localhost:8080"),
                env.getOrDefault("LOTTERY_MOCK_PAYMENT_WEBHOOK_SECRET", "local-mock-payment-secret"));
    }

    private static int httpPort(Map<String, String> env) {
        String value = firstNonBlank(env, "LOTTERY_HTTP_PORT", "PORT");
        return value == null ? 8080 : Integer.parseInt(value);
    }

    private static DatabaseConnection databaseConnection(Map<String, String> env) {
        String jdbcUrl = stringValue(env, "LOTTERY_JDBC_URL");
        if (jdbcUrl != null) {
            return new DatabaseConnection(
                    jdbcUrl,
                    stringValueOrDefault(env, "LOTTERY_JDBC_USER", "lottery"),
                    stringValueOrDefault(env, "LOTTERY_JDBC_PASSWORD", "lottery"));
        }

        String databaseUrl = stringValue(env, "DATABASE_URL");
        if (databaseUrl != null) {
            return databaseConnectionFromUrl(databaseUrl, env);
        }

        return new DatabaseConnection(
                "jdbc:postgresql://localhost:5432/lottery",
                stringValueOrDefault(env, "LOTTERY_JDBC_USER", "lottery"),
                stringValueOrDefault(env, "LOTTERY_JDBC_PASSWORD", "lottery"));
    }

    private static DatabaseConnection databaseConnectionFromUrl(String databaseUrl, Map<String, String> env) {
        URI uri = URI.create(databaseUrl);
        String scheme = uri.getScheme();
        if (!"postgresql".equals(scheme) && !"postgres".equals(scheme)) {
            throw new IllegalArgumentException("Unsupported DATABASE_URL scheme: " + scheme);
        }

        String rawUserInfo = uri.getRawUserInfo();
        String parsedUser = null;
        String parsedPassword = null;
        if (rawUserInfo != null) {
            int separator = rawUserInfo.indexOf(':');
            if (separator >= 0) {
                parsedUser = decode(rawUserInfo.substring(0, separator));
                parsedPassword = decode(rawUserInfo.substring(separator + 1));
            } else {
                parsedUser = decode(rawUserInfo);
            }
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://");
        jdbcUrl.append(uri.getHost());
        if (uri.getPort() > -1) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        String path = uri.getRawPath();
        jdbcUrl.append(path == null || path.isBlank() ? "/" : path);
        String query = uri.getRawQuery();
        if (query != null && !query.isBlank()) {
            jdbcUrl.append('?').append(query);
        }

        return new DatabaseConnection(
                jdbcUrl.toString(),
                stringValueOrDefault(env, "LOTTERY_JDBC_USER", parsedUser == null ? "lottery" : parsedUser),
                stringValueOrDefault(env, "LOTTERY_JDBC_PASSWORD", parsedPassword == null ? "lottery" : parsedPassword));
    }

    private static int intValue(Map<String, String> env, String key, int defaultValue) {
        String value = stringValue(env, key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    private static boolean booleanValue(Map<String, String> env, String key, boolean defaultValue) {
        String value = stringValue(env, key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private static String firstNonBlank(Map<String, String> env, String... keys) {
        for (String key : keys) {
            String value = stringValue(env, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String stringValueOrDefault(Map<String, String> env, String key, String defaultValue) {
        String value = stringValue(env, key);
        return value == null ? defaultValue : value;
    }

    private static String stringValue(Map<String, String> env, String key) {
        String value = env.get(key);
        return value == null || value.isBlank() ? null : value;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record DatabaseConnection(String jdbcUrl, String user, String password) {
    }
}
