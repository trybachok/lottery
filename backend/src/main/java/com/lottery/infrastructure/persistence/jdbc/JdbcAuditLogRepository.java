package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.AuditLog;
import com.lottery.domain.repository.AuditLogRepository;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class JdbcAuditLogRepository implements AuditLogRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcAuditLogRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void append(AuditLog auditLog) {
        String sql = """
                insert into audit_logs (
                  id, actor_user_id, actor_role_codes, action, entity_type, entity_id, request_id,
                  ip_address, user_agent, before_json, after_json, created_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?::inet, ?, ?::jsonb, ?::jsonb, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, auditLog.id());
                statement.setObject(2, auditLog.actorUserId());
                statement.setArray(3, connection.createArrayOf("text", auditLog.actorRoleCodes().toArray(String[]::new)));
                statement.setString(4, auditLog.action());
                statement.setString(5, auditLog.entityType());
                statement.setObject(6, auditLog.entityId());
                statement.setString(7, auditLog.requestId());
                statement.setString(8, auditLog.ipAddress());
                statement.setString(9, auditLog.userAgent());
                statement.setString(10, auditLog.beforeJson());
                statement.setString(11, auditLog.afterJson());
                JdbcSupport.setInstant(statement, 12, auditLog.createdAt());
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to append audit log", exception);
        }
    }

    @Override
    public List<AuditLog> find(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Instant createdFrom,
            Instant createdTo,
            int limit,
            int offset) {
        StringBuilder sql = new StringBuilder("""
                select id, actor_user_id, actor_role_codes, action, entity_type, entity_id, request_id,
                       ip_address::text as ip_address, user_agent, before_json::text as before_json,
                       after_json::text as after_json, created_at
                from audit_logs
                where 1 = 1
                """);
        List<SqlParameter> parameters = new ArrayList<>();
        if (actorUserId != null) {
            sql.append(" and actor_user_id = ?");
            parameters.add((statement, index) -> statement.setObject(index, actorUserId));
        }
        if (action != null) {
            sql.append(" and action = ?");
            parameters.add((statement, index) -> statement.setString(index, action));
        }
        if (entityType != null) {
            sql.append(" and entity_type = ?");
            parameters.add((statement, index) -> statement.setString(index, entityType));
        }
        if (entityId != null) {
            sql.append(" and entity_id = ?");
            parameters.add((statement, index) -> statement.setObject(index, entityId));
        }
        if (createdFrom != null) {
            sql.append(" and created_at >= ?");
            parameters.add((statement, index) -> JdbcSupport.setInstant(statement, index, createdFrom));
        }
        if (createdTo != null) {
            sql.append(" and created_at <= ?");
            parameters.add((statement, index) -> JdbcSupport.setInstant(statement, index, createdTo));
        }
        sql.append(" order by created_at desc limit ? offset ?");
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                int index = 1;
                for (SqlParameter parameter : parameters) {
                    parameter.bind(statement, index++);
                }
                statement.setInt(index++, limit);
                statement.setInt(index, offset);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<AuditLog> auditLogs = new ArrayList<>();
                    while (resultSet.next()) {
                        auditLogs.add(map(resultSet));
                    }
                    return auditLogs;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list audit logs", exception);
        }
    }

    private AuditLog map(ResultSet resultSet) throws SQLException {
        Array rolesArray = resultSet.getArray("actor_role_codes");
        String[] roleCodes = rolesArray == null ? new String[0] : (String[]) rolesArray.getArray();
        return new AuditLog(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("actor_user_id", UUID.class),
                Arrays.asList(roleCodes),
                resultSet.getString("action"),
                resultSet.getString("entity_type"),
                resultSet.getObject("entity_id", UUID.class),
                resultSet.getString("request_id"),
                resultSet.getString("ip_address"),
                resultSet.getString("user_agent"),
                resultSet.getString("before_json"),
                resultSet.getString("after_json"),
                resultSet.getTimestamp("created_at").toInstant());
    }

    @FunctionalInterface
    private interface SqlParameter {
        void bind(PreparedStatement statement, int index) throws SQLException;
    }
}
