package com.lottery.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.domain.model.SystemSetting;
import com.lottery.domain.repository.SystemSettingsRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

public final class JdbcSystemSettingsRepository implements SystemSettingsRepository {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public JdbcSystemSettingsRepository(JdbcConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SystemSetting> findByKey(String key) {
        String sql = """
                select key, value_json::text as value_json, updated_by, updated_at
                from system_settings
                where key = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, key);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new SystemSetting(
                            resultSet.getString("key"),
                            fromJson(resultSet.getString("value_json")),
                            resultSet.getObject("updated_by", java.util.UUID.class),
                            resultSet.getTimestamp("updated_at").toInstant()));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find system setting", exception);
        }
    }

    @Override
    public SystemSetting save(SystemSetting setting) {
        String sql = """
                insert into system_settings (key, value_json, updated_by, updated_at)
                values (?, ?::jsonb, ?, ?)
                on conflict (key) do update
                set value_json = excluded.value_json,
                    updated_by = excluded.updated_by,
                    updated_at = excluded.updated_at
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, setting.key());
                statement.setString(2, toJson(setting.value()));
                statement.setObject(3, setting.updatedBy());
                statement.setTimestamp(4, Timestamp.from(setting.updatedAt()));
                statement.executeUpdate();
            }
            return findByKey(setting.key()).orElseThrow();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save system setting", exception);
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse system setting JSON", exception);
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize system setting JSON", exception);
        }
    }
}
