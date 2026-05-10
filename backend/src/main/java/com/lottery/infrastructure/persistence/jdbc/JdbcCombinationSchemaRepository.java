package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.valueobject.CombinationSchemaDefinition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

public final class JdbcCombinationSchemaRepository implements CombinationSchemaRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcCombinationSchemaRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public CombinationSchema save(CombinationSchema schema) {
        String sql = """
                insert into combination_schemas (id, name, schema_json, created_at)
                values (?, ?, ?::jsonb, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, schema.id());
                statement.setString(2, schema.name());
                statement.setString(3, schema.definition().document());
                statement.setTimestamp(4, Timestamp.from(schema.createdAt()));
                statement.executeUpdate();
                return schema;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save combination schema", exception);
        }
    }

    @Override
    public Optional<CombinationSchema> findById(UUID id) {
        String sql = """
                select id, name, schema_json::text as schema_json, created_at
                from combination_schemas
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
                    return Optional.of(new CombinationSchema(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getString("name"),
                            new CombinationSchemaDefinition(resultSet.getString("schema_json")),
                            resultSet.getTimestamp("created_at").toInstant()));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find combination schema", exception);
        }
    }
}
