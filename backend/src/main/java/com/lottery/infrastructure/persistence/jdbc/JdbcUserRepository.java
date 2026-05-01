package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.User;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.valueobject.UserStatus;
import com.lottery.application.OptimisticLockException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcUserRepository implements UserRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcUserRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public User save(User user) {
        String sql = """
                insert into users (id, email, login, password_hash, status, created_at, updated_at, deleted_at, version)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, user.id());
                statement.setString(2, user.email());
                statement.setString(3, user.login());
                statement.setString(4, user.passwordHash().orElse(null));
                statement.setString(5, user.status().name());
                JdbcSupport.setInstant(statement, 6, user.createdAt());
                JdbcSupport.setInstant(statement, 7, user.updatedAt());
                JdbcSupport.setInstant(statement, 8, user.deletedAt().orElse(null));
                statement.setLong(9, user.version());
                statement.executeUpdate();
                return user;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save user", exception);
        }
    }

    @Override
    public User update(User user) {
        String sql = """
                update users
                set email = ?, login = ?, password_hash = ?, status = ?, updated_at = ?, deleted_at = ?, version = version + 1
                where id = ? and version = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.email());
                statement.setString(2, user.login());
                statement.setString(3, user.passwordHash().orElse(null));
                statement.setString(4, user.status().name());
                JdbcSupport.setInstant(statement, 5, user.updatedAt());
                JdbcSupport.setInstant(statement, 6, user.deletedAt().orElse(null));
                statement.setObject(7, user.id());
                statement.setLong(8, user.version());
                int updated = statement.executeUpdate();
                if (updated != 1) {
                    throw new OptimisticLockException("User");
                }
                return new User(
                        user.id(),
                        user.email(),
                        user.login(),
                        user.passwordHash().orElse(null),
                        user.status(),
                        user.createdAt(),
                        user.updatedAt(),
                        user.deletedAt().orElse(null),
                        user.version() + 1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update user", exception);
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        String sql = """
                select id, email, login, password_hash, status, created_at, updated_at, deleted_at, version
                from users
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
            throw new IllegalStateException("Failed to find user", exception);
        }
    }

    @Override
    public Optional<User> findByEmailOrLogin(String loginOrEmail) {
        String sql = """
                select id, email, login, password_hash, status, created_at, updated_at, deleted_at, version
                from users
                where deleted_at is null and (email = ? or login = ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, loginOrEmail);
                statement.setString(2, loginOrEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find user by login or email", exception);
        }
    }

    @Override
    public List<User> findAll(int limit, int offset) {
        String sql = """
                select id, email, login, password_hash, status, created_at, updated_at, deleted_at, version
                from users
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
                    List<User> users = new ArrayList<>();
                    while (resultSet.next()) {
                        users.add(map(resultSet));
                    }
                    return users;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list users", exception);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists("select 1 from users where email = ? and deleted_at is null", email);
    }

    @Override
    public boolean existsByLogin(String login) {
        return exists("select 1 from users where login = ? and deleted_at is null", login);
    }

    @Override
    public boolean existsByEmailExceptId(String email, UUID id) {
        return existsExceptId("select 1 from users where email = ? and id <> ? and deleted_at is null", email, id);
    }

    @Override
    public boolean existsByLoginExceptId(String login, UUID id) {
        return existsExceptId("select 1 from users where login = ? and id <> ? and deleted_at is null", login, id);
    }

    private boolean exists(String sql, String value) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, value);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check user uniqueness", exception);
        }
    }

    private boolean existsExceptId(String sql, String value, UUID id) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, value);
                statement.setObject(2, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check user uniqueness", exception);
        }
    }

    private User map(ResultSet resultSet) throws SQLException {
        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        return new User(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("email"),
                resultSet.getString("login"),
                resultSet.getString("password_hash"),
                UserStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                deletedAt == null ? null : deletedAt.toInstant(),
                resultSet.getLong("version"));
    }
}
