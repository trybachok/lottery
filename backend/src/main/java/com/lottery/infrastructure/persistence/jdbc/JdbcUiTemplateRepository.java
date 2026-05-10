package com.lottery.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.domain.model.UiTemplate;
import com.lottery.domain.repository.UiTemplateRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class JdbcUiTemplateRepository implements UiTemplateRepository {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public JdbcUiTemplateRepository(JdbcConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<UiTemplate> findAll(int limit, int offset) {
        String sql = """
                select id, name, layout_json::text as layout_json, created_at
                from ui_templates
                order by created_at desc
                limit ? offset ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<UiTemplate> templates = new java.util.ArrayList<>();
                    while (resultSet.next()) {
                        templates.add(map(resultSet));
                    }
                    return templates;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list UI templates", exception);
        }
    }

    @Override
    public Optional<UiTemplate> findById(UUID id) {
        String sql = """
                select id, name, layout_json::text as layout_json, created_at
                from ui_templates
                where id = ?
                """;
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
            throw new IllegalStateException("Failed to find UI template", exception);
        }
    }

    @Override
    public UiTemplate save(UiTemplate template) {
        String sql = """
                insert into ui_templates (id, name, layout_json, created_at)
                values (?, ?, ?::jsonb, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, template.id());
                statement.setString(2, template.name());
                statement.setString(3, toJson(template.layout()));
                statement.setTimestamp(4, Timestamp.from(template.createdAt()));
                statement.executeUpdate();
            }
            return findById(template.id()).orElseThrow();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save UI template", exception);
        }
    }

    @Override
    public UiTemplate update(UiTemplate template) {
        String sql = """
                update ui_templates
                set name = ?, layout_json = ?::jsonb
                where id = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, template.name());
                statement.setString(2, toJson(template.layout()));
                statement.setObject(3, template.id());
                if (statement.executeUpdate() == 0) {
                    throw new IllegalStateException("UI template was not updated");
                }
            }
            return findById(template.id()).orElseThrow();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update UI template", exception);
        }
    }

    private UiTemplate map(ResultSet resultSet) throws SQLException {
        return new UiTemplate(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                fromJson(resultSet.getString("layout_json")),
                resultSet.getTimestamp("created_at").toInstant());
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse UI template JSON", exception);
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize UI template JSON", exception);
        }
    }
}
