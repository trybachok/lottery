package com.lottery.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.domain.model.DrawResult;
import com.lottery.domain.repository.DrawResultRepository;
import com.lottery.domain.valueobject.Combination;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcDrawResultRepository implements DrawResultRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public JdbcDrawResultRepository(JdbcConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public DrawResult save(DrawResult drawResult) {
        String sql = """
                insert into draw_results (
                  id, draw_id, winning_combination_json, algorithm_version, random_provider, proof_hash, generated_by, generated_at
                )
                values (?, ?, ?::jsonb, ?, ?, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, drawResult.id());
                statement.setObject(2, drawResult.drawId());
                statement.setString(3, objectMapper.writeValueAsString(drawResult.winningCombination().values()));
                statement.setString(4, drawResult.algorithmVersion());
                statement.setString(5, drawResult.randomProvider());
                statement.setString(6, drawResult.maybeProofHash().orElse(null));
                statement.setObject(7, drawResult.generatedBy());
                JdbcSupport.setInstant(statement, 8, drawResult.generatedAt());
                statement.executeUpdate();
                return drawResult;
            }
        } catch (SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Failed to save draw result", exception);
        }
    }

    @Override
    public Optional<DrawResult> findByDrawId(UUID drawId) {
        String sql = """
                select id, draw_id, winning_combination_json, algorithm_version, random_provider, proof_hash, generated_by, generated_at
                from draw_results
                where draw_id = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, drawId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        } catch (SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Failed to find draw result", exception);
        }
    }

    @Override
    public boolean existsByDrawId(UUID drawId) {
        String sql = "select 1 from draw_results where draw_id = ?";
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, drawId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check draw result existence", exception);
        }
    }

    private DrawResult map(ResultSet resultSet) throws SQLException, JsonProcessingException {
        return new DrawResult(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("draw_id", UUID.class),
                new Combination(objectMapper.readValue(resultSet.getString("winning_combination_json"), STRING_LIST)),
                resultSet.getString("algorithm_version"),
                resultSet.getString("random_provider"),
                resultSet.getString("proof_hash"),
                resultSet.getObject("generated_by", UUID.class),
                resultSet.getTimestamp("generated_at").toInstant());
    }
}
