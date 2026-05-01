package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.repository.RbacRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class JdbcRbacRepository implements RbacRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcRbacRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Set<String> findPermissionCodesByUserId(UUID userId) {
        String sql = """
                select distinct p.code
                from user_roles ur
                join role_permissions rp on rp.role_id = ur.role_id
                join permissions p on p.id = rp.permission_id
                where ur.user_id = ?
                """;
        return findCodes(sql, userId);
    }

    @Override
    public Set<String> findRoleCodesByUserId(UUID userId) {
        String sql = """
                select distinct r.code
                from user_roles ur
                join roles r on r.id = ur.role_id
                where ur.user_id = ?
                """;
        return findCodes(sql, userId);
    }

    @Override
    public void assignRoleByCode(UUID userId, String roleCode) {
        String sql = """
                insert into user_roles (user_id, role_id)
                select ?, id from roles where code = ?
                on conflict do nothing
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, userId);
                statement.setString(2, roleCode);
                int inserted = statement.executeUpdate();
                if (inserted != 1) {
                    throw new IllegalStateException("Role was not assigned: " + roleCode);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to assign role", exception);
        }
    }

    private Set<String> findCodes(String sql, UUID userId) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    Set<String> codes = new HashSet<>();
                    while (resultSet.next()) {
                        codes.add(resultSet.getString(1));
                    }
                    return Set.copyOf(codes);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load RBAC codes", exception);
        }
    }
}
