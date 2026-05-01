package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.CombinationSchema;
import com.lottery.domain.repository.CombinationSchemaRepository;
import com.lottery.domain.valueobject.CombinationSchemaDefinition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class JdbcCombinationSchemaRepository implements CombinationSchemaRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcCombinationSchemaRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
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
