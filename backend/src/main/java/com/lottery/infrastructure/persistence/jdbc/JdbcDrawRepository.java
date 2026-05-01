package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.application.OptimisticLockException;
import com.lottery.domain.model.Draw;
import com.lottery.domain.repository.DrawRepository;
import com.lottery.domain.valueobject.DrawStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcDrawRepository implements DrawRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcDrawRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Draw save(Draw draw) {
        String sql = """
                insert into draws (
                  id, title, description, status, manager_id, combination_schema_id, ui_theme_id, ui_template_id,
                  sales_start_at, sales_end_at, draw_at, max_tickets, is_test, created_at, updated_at, deleted_at, version
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, draw.id());
                statement.setString(2, draw.title());
                statement.setString(3, draw.description());
                statement.setString(4, draw.status().name());
                statement.setObject(5, draw.managerId().orElse(null));
                statement.setObject(6, draw.combinationSchemaId());
                statement.setObject(7, draw.uiThemeId().orElse(null));
                statement.setObject(8, draw.uiTemplateId().orElse(null));
                JdbcSupport.setInstant(statement, 9, draw.salesStartAt());
                JdbcSupport.setInstant(statement, 10, draw.salesEndAt());
                JdbcSupport.setInstant(statement, 11, draw.drawAt());
                if (draw.maxTickets().isPresent()) {
                    statement.setInt(12, draw.maxTickets().get());
                } else {
                    statement.setObject(12, null);
                }
                statement.setBoolean(13, draw.test());
                JdbcSupport.setInstant(statement, 14, draw.createdAt());
                JdbcSupport.setInstant(statement, 15, draw.updatedAt());
                JdbcSupport.setInstant(statement, 16, draw.deletedAt().orElse(null));
                statement.setLong(17, draw.version());
                statement.executeUpdate();
                return draw;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save draw", exception);
        }
    }

    @Override
    public Draw update(Draw draw) {
        String sql = """
                update draws
                set title = ?, description = ?, status = ?, manager_id = ?, combination_schema_id = ?,
                    ui_theme_id = ?, ui_template_id = ?, sales_start_at = ?, sales_end_at = ?, draw_at = ?,
                    max_tickets = ?, is_test = ?, updated_at = ?, deleted_at = ?, version = version + 1
                where id = ? and version = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, draw.title());
                statement.setString(2, draw.description());
                statement.setString(3, draw.status().name());
                statement.setObject(4, draw.managerId().orElse(null));
                statement.setObject(5, draw.combinationSchemaId());
                statement.setObject(6, draw.uiThemeId().orElse(null));
                statement.setObject(7, draw.uiTemplateId().orElse(null));
                JdbcSupport.setInstant(statement, 8, draw.salesStartAt());
                JdbcSupport.setInstant(statement, 9, draw.salesEndAt());
                JdbcSupport.setInstant(statement, 10, draw.drawAt());
                if (draw.maxTickets().isPresent()) {
                    statement.setInt(11, draw.maxTickets().get());
                } else {
                    statement.setObject(11, null);
                }
                statement.setBoolean(12, draw.test());
                JdbcSupport.setInstant(statement, 13, draw.updatedAt());
                JdbcSupport.setInstant(statement, 14, draw.deletedAt().orElse(null));
                statement.setObject(15, draw.id());
                statement.setLong(16, draw.version());
                int updated = statement.executeUpdate();
                if (updated != 1) {
                    throw new OptimisticLockException("Draw");
                }
                return new Draw(
                        draw.id(),
                        draw.title(),
                        draw.description(),
                        draw.status(),
                        draw.managerId().orElse(null),
                        draw.combinationSchemaId(),
                        draw.uiThemeId().orElse(null),
                        draw.uiTemplateId().orElse(null),
                        draw.salesStartAt(),
                        draw.salesEndAt(),
                        draw.drawAt(),
                        draw.maxTickets().orElse(null),
                        draw.test(),
                        draw.createdAt(),
                        draw.updatedAt(),
                        draw.deletedAt().orElse(null),
                        draw.version() + 1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update draw", exception);
        }
    }

    @Override
    public Optional<Draw> findById(UUID id) {
        String sql = """
                select id, title, description, status, manager_id, combination_schema_id, ui_theme_id, ui_template_id,
                       sales_start_at, sales_end_at, draw_at, max_tickets, is_test, created_at, updated_at, deleted_at, version
                from draws
                where id = ? and deleted_at is null
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find draw", exception);
        }
    }

    @Override
    public List<Draw> findAll(int limit, int offset) {
        String sql = """
                select id, title, description, status, manager_id, combination_schema_id, ui_theme_id, ui_template_id,
                       sales_start_at, sales_end_at, draw_at, max_tickets, is_test, created_at, updated_at, deleted_at, version
                from draws
                where deleted_at is null
                order by created_at desc
                limit ? offset ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Draw> draws = new ArrayList<>();
                    while (resultSet.next()) {
                        draws.add(map(resultSet));
                    }
                    return draws;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list draws", exception);
        }
    }

    private Draw map(ResultSet resultSet) throws SQLException {
        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        Integer maxTickets = resultSet.getObject("max_tickets") == null ? null : resultSet.getInt("max_tickets");
        return new Draw(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("title"),
                resultSet.getString("description"),
                DrawStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("manager_id", UUID.class),
                resultSet.getObject("combination_schema_id", UUID.class),
                resultSet.getObject("ui_theme_id", UUID.class),
                resultSet.getObject("ui_template_id", UUID.class),
                resultSet.getTimestamp("sales_start_at").toInstant(),
                resultSet.getTimestamp("sales_end_at").toInstant(),
                resultSet.getTimestamp("draw_at").toInstant(),
                maxTickets,
                resultSet.getBoolean("is_test"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                deletedAt == null ? null : deletedAt.toInstant(),
                resultSet.getLong("version"));
    }
}
