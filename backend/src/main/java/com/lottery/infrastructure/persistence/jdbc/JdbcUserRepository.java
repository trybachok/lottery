package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.User;
import com.lottery.domain.repository.UserRepository;
import com.lottery.domain.valueobject.UserStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
    public boolean existsByEmail(String email) {
        return exists("select 1 from users where email = ? and deleted_at is null", email);
    }

    @Override
    public boolean existsByLogin(String login) {
        return exists("select 1 from users where login = ? and deleted_at is null", login);
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
