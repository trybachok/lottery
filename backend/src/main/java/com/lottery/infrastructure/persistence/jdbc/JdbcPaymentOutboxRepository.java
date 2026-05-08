package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.PaymentOutboxMessage;
import com.lottery.domain.repository.PaymentOutboxRepository;
import com.lottery.domain.valueobject.PaymentOutboxStatus;
import com.lottery.domain.valueobject.PaymentOutboxType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcPaymentOutboxRepository implements PaymentOutboxRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcPaymentOutboxRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public PaymentOutboxMessage save(PaymentOutboxMessage message) {
        String sql = """
                insert into payment_outbox (
                  id, type, status, invoice_id, payment_id, provider_code, payload_json, attempts,
                  next_attempt_at, last_error, created_at, updated_at, processed_at
                )
                values (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, message);
                statement.executeUpdate();
                return message;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save payment outbox message", exception);
        }
    }

    @Override
    public PaymentOutboxMessage update(PaymentOutboxMessage message) {
        String sql = """
                update payment_outbox
                set type = ?, status = ?, invoice_id = ?, payment_id = ?, provider_code = ?, payload_json = ?::jsonb,
                    attempts = ?, next_attempt_at = ?, last_error = ?, updated_at = ?, processed_at = ?
                where id = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, message.type().name());
                statement.setString(2, message.status().name());
                statement.setObject(3, message.invoiceId());
                statement.setObject(4, message.maybePaymentId().orElse(null));
                statement.setString(5, message.providerCode());
                statement.setString(6, message.payloadJson());
                statement.setInt(7, message.attempts());
                JdbcSupport.setInstant(statement, 8, message.nextAttemptAt());
                statement.setString(9, message.maybeLastError().orElse(null));
                JdbcSupport.setInstant(statement, 10, message.updatedAt());
                JdbcSupport.setInstant(statement, 11, message.maybeProcessedAt().orElse(null));
                statement.setObject(12, message.id());
                statement.executeUpdate();
                return message;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update payment outbox message", exception);
        }
    }

    @Override
    public Optional<PaymentOutboxMessage> findById(UUID id) {
        return findOne("""
                select id, type, status, invoice_id, payment_id, provider_code, payload_json, attempts,
                       next_attempt_at, last_error, created_at, updated_at, processed_at
                from payment_outbox
                where id = ?
                """, statement -> statement.setObject(1, id));
    }

    @Override
    public List<PaymentOutboxMessage> findDueForProcessing(Instant now, int limit) {
        String sql = """
                select id, type, status, invoice_id, payment_id, provider_code, payload_json, attempts,
                       next_attempt_at, last_error, created_at, updated_at, processed_at
                from payment_outbox
                where status in ('PENDING', 'FAILED') and next_attempt_at <= ?
                order by created_at asc
                limit ?
                for update skip locked
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                JdbcSupport.setInstant(statement, 1, now);
                statement.setInt(2, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<PaymentOutboxMessage> messages = new ArrayList<>();
                    while (resultSet.next()) {
                        messages.add(map(resultSet));
                    }
                    return messages;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list due payment outbox messages", exception);
        }
    }

    private Optional<PaymentOutboxMessage> findOne(String sql, StatementBinder binder) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find payment outbox message", exception);
        }
    }

    private void bind(PreparedStatement statement, PaymentOutboxMessage message) throws SQLException {
        statement.setObject(1, message.id());
        statement.setString(2, message.type().name());
        statement.setString(3, message.status().name());
        statement.setObject(4, message.invoiceId());
        statement.setObject(5, message.maybePaymentId().orElse(null));
        statement.setString(6, message.providerCode());
        statement.setString(7, message.payloadJson());
        statement.setInt(8, message.attempts());
        JdbcSupport.setInstant(statement, 9, message.nextAttemptAt());
        statement.setString(10, message.maybeLastError().orElse(null));
        JdbcSupport.setInstant(statement, 11, message.createdAt());
        JdbcSupport.setInstant(statement, 12, message.updatedAt());
        JdbcSupport.setInstant(statement, 13, message.maybeProcessedAt().orElse(null));
    }

    private PaymentOutboxMessage map(ResultSet resultSet) throws SQLException {
        Timestamp processedAt = resultSet.getTimestamp("processed_at");
        return new PaymentOutboxMessage(
                resultSet.getObject("id", UUID.class),
                PaymentOutboxType.valueOf(resultSet.getString("type")),
                PaymentOutboxStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("invoice_id", UUID.class),
                resultSet.getObject("payment_id", UUID.class),
                resultSet.getString("provider_code"),
                resultSet.getString("payload_json"),
                resultSet.getInt("attempts"),
                resultSet.getTimestamp("next_attempt_at").toInstant(),
                resultSet.getString("last_error"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                processedAt == null ? null : processedAt.toInstant());
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
