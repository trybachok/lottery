package com.lottery.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.domain.model.UiTheme;
import com.lottery.domain.repository.UiThemeRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JdbcUiThemeRepository implements UiThemeRepository {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public JdbcUiThemeRepository(JdbcConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<UiTheme> findAll(int limit, int offset) {
        String sql = """
                select id, name, tokens_json::text as tokens_json, is_default, created_at
                from ui_themes
                order by is_default desc, created_at desc
                limit ? offset ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<UiTheme> themes = new java.util.ArrayList<>();
                    while (resultSet.next()) {
                        themes.add(map(resultSet));
                    }
                    return themes;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list UI themes", exception);
        }
    }

    @Override
    public Optional<UiTheme> findById(UUID id) {
        String sql = """
                select id, name, tokens_json::text as tokens_json, is_default, created_at
                from ui_themes
                where id = ?
                """;
        return findOne(sql, id);
    }

    @Override
    public Optional<UiTheme> findDefault() {
        String sql = """
                select id, name, tokens_json::text as tokens_json, is_default, created_at
                from ui_themes
                where is_default = true
                order by created_at desc
                limit 1
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find default UI theme", exception);
        }
    }

    @Override
    public UiTheme save(UiTheme theme) {
        String sql = """
                insert into ui_themes (id, name, tokens_json, is_default, created_at)
                values (?, ?, ?::jsonb, ?, ?)
                """;
        executeWrite(sql, theme);
        return findById(theme.id()).orElseThrow();
    }

    @Override
    public UiTheme update(UiTheme theme) {
        String sql = """
                update ui_themes
                set name = ?, tokens_json = ?::jsonb
                where id = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, theme.name());
                statement.setString(2, toJson(theme.tokens()));
                statement.setObject(3, theme.id());
                if (statement.executeUpdate() == 0) {
                    throw new IllegalStateException("UI theme was not updated");
                }
            }
            return findById(theme.id()).orElseThrow();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update UI theme", exception);
        }
    }

    @Override
    public void setDefault(UUID id) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement reset = connection.prepareStatement("update ui_themes set is_default = false")) {
                reset.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("update ui_themes set is_default = true where id = ?")) {
                statement.setObject(1, id);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalStateException("Default UI theme was not updated");
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to set default UI theme", exception);
        }
    }

    private Optional<UiTheme> findOne(String sql, UUID id) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find UI theme", exception);
        }
    }

    private void executeWrite(String sql, UiTheme theme) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, theme.id());
                statement.setString(2, theme.name());
                statement.setString(3, toJson(theme.tokens()));
                statement.setBoolean(4, theme.defaultTheme());
                statement.setTimestamp(5, Timestamp.from(theme.createdAt()));
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save UI theme", exception);
        }
    }

    private UiTheme map(ResultSet resultSet) throws SQLException {
        return new UiTheme(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                fromJson(resultSet.getString("tokens_json")),
                resultSet.getBoolean("is_default"),
                resultSet.getTimestamp("created_at").toInstant());
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse UI theme JSON", exception);
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize UI theme JSON", exception);
        }
    }
}
