package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.repository.RbacRepository;
import com.lottery.domain.model.Permission;
import com.lottery.domain.model.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
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

    @Override
    public boolean existsUserWithRoleCode(String roleCode) {
        String sql = """
                select 1
                from user_roles ur
                join roles r on r.id = ur.role_id
                join users u on u.id = ur.user_id
                where r.code = ? and u.deleted_at is null
                limit 1
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, roleCode);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check role assignment existence", exception);
        }
    }

    @Override
    public List<Role> findAllRoles(int limit, int offset) {
        String sql = "select id, code, name, description, is_system from roles order by code limit ? offset ?";
        return findRoles(sql, statement -> {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
        });
    }

    @Override
    public Optional<Role> findRoleById(UUID id) {
        String sql = "select id, code, name, description, is_system from roles where id = ?";
        return findRoles(sql, statement -> statement.setObject(1, id)).stream().findFirst();
    }

    @Override
    public Role saveRole(Role role) {
        String sql = "insert into roles (id, code, name, description, is_system) values (?, ?, ?, ?, ?)";
        execute(sql, statement -> bindRole(statement, role));
        return role;
    }

    @Override
    public Role updateRole(Role role) {
        String sql = "update roles set code = ?, name = ?, description = ? where id = ?";
        int updated = execute(sql, statement -> {
            statement.setString(1, role.code());
            statement.setString(2, role.name());
            statement.setString(3, role.description());
            statement.setObject(4, role.id());
        });
        if (updated != 1) {
            throw new IllegalStateException("Role was not updated");
        }
        return role;
    }

    @Override
    public void deleteRole(UUID id) {
        execute("delete from roles where id = ?", statement -> statement.setObject(1, id));
    }

    @Override
    public List<Permission> findAllPermissions(int limit, int offset) {
        String sql = "select id, code, description from permissions order by code limit ? offset ?";
        return findPermissions(sql, statement -> {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
        });
    }

    @Override
    public Optional<Permission> findPermissionById(UUID id) {
        String sql = "select id, code, description from permissions where id = ?";
        return findPermissions(sql, statement -> statement.setObject(1, id)).stream().findFirst();
    }

    @Override
    public Permission savePermission(Permission permission) {
        String sql = "insert into permissions (id, code, description) values (?, ?, ?)";
        execute(sql, statement -> {
            statement.setObject(1, permission.id());
            statement.setString(2, permission.code());
            statement.setString(3, permission.description());
        });
        return permission;
    }

    @Override
    public Permission updatePermission(Permission permission) {
        int updated = execute("update permissions set code = ?, description = ? where id = ?", statement -> {
            statement.setString(1, permission.code());
            statement.setString(2, permission.description());
            statement.setObject(3, permission.id());
        });
        if (updated != 1) {
            throw new IllegalStateException("Permission was not updated");
        }
        return permission;
    }

    @Override
    public void deletePermission(UUID id) {
        execute("delete from permissions where id = ?", statement -> statement.setObject(1, id));
    }

    @Override
    public List<Role> findRolesByUserId(UUID userId) {
        String sql = """
                select r.id, r.code, r.name, r.description, r.is_system
                from user_roles ur join roles r on r.id = ur.role_id
                where ur.user_id = ?
                order by r.code
                """;
        return findRoles(sql, statement -> statement.setObject(1, userId));
    }

    @Override
    public void assignRole(UUID userId, UUID roleId) {
        execute(
                "insert into user_roles (user_id, role_id) values (?, ?) on conflict do nothing",
                statement -> {
                    statement.setObject(1, userId);
                    statement.setObject(2, roleId);
                });
    }

    @Override
    public void removeRole(UUID userId, UUID roleId) {
        execute("delete from user_roles where user_id = ? and role_id = ?", statement -> {
            statement.setObject(1, userId);
            statement.setObject(2, roleId);
        });
    }

    @Override
    public List<Permission> findPermissionsByRoleId(UUID roleId) {
        String sql = """
                select p.id, p.code, p.description
                from role_permissions rp join permissions p on p.id = rp.permission_id
                where rp.role_id = ?
                order by p.code
                """;
        return findPermissions(sql, statement -> statement.setObject(1, roleId));
    }

    @Override
    public void assignPermission(UUID roleId, UUID permissionId) {
        execute(
                "insert into role_permissions (role_id, permission_id) values (?, ?) on conflict do nothing",
                statement -> {
                    statement.setObject(1, roleId);
                    statement.setObject(2, permissionId);
                });
    }

    @Override
    public void removePermission(UUID roleId, UUID permissionId) {
        execute("delete from role_permissions where role_id = ? and permission_id = ?", statement -> {
            statement.setObject(1, roleId);
            statement.setObject(2, permissionId);
        });
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

    private List<Role> findRoles(String sql, SqlBinder binder) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Role> roles = new ArrayList<>();
                    while (resultSet.next()) {
                        roles.add(new Role(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getString("code"),
                                resultSet.getString("name"),
                                resultSet.getString("description"),
                                resultSet.getBoolean("is_system")));
                    }
                    return List.copyOf(roles);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load roles", exception);
        }
    }

    private List<Permission> findPermissions(String sql, SqlBinder binder) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Permission> permissions = new ArrayList<>();
                    while (resultSet.next()) {
                        permissions.add(new Permission(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getString("code"),
                                resultSet.getString("description")));
                    }
                    return List.copyOf(permissions);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load permissions", exception);
        }
    }

    private int execute(String sql, SqlBinder binder) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                return statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update RBAC", exception);
        }
    }

    private void bindRole(PreparedStatement statement, Role role) throws SQLException {
        statement.setObject(1, role.id());
        statement.setString(2, role.code());
        statement.setString(3, role.name());
        statement.setString(4, role.description());
        statement.setBoolean(5, role.system());
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
