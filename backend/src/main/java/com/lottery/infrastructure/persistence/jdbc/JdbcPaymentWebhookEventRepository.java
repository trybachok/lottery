package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.PaymentWebhookEvent;
import com.lottery.domain.repository.PaymentWebhookEventRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

public final class JdbcPaymentWebhookEventRepository implements PaymentWebhookEventRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcPaymentWebhookEventRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void acquireProcessingLock(String providerCode, String externalEventId) {
        String sql = "select pg_advisory_xact_lock(hashtext(?))";
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, providerCode + ":" + externalEventId);
                statement.execute();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to acquire webhook processing lock", exception);
        }
    }

    @Override
    public PaymentWebhookEvent save(PaymentWebhookEvent event) {
        String sql = """
                insert into payment_webhook_events (
                  id, provider_code, event_type, external_event_id, payload, signature_valid, processed, received_at
                )
                values (?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, event);
                statement.executeUpdate();
                return event;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save payment webhook event", exception);
        }
    }

    @Override
    public PaymentWebhookEvent update(PaymentWebhookEvent event) {
        String sql = """
                update payment_webhook_events
                set processed = ?
                where id = ? and received_at = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBoolean(1, event.processed());
                statement.setObject(2, event.id());
                JdbcSupport.setInstant(statement, 3, event.receivedAt());
                statement.executeUpdate();
                return event;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update payment webhook event", exception);
        }
    }

    @Override
    public Optional<PaymentWebhookEvent> findProcessedByProviderAndExternalEventId(String providerCode, String externalEventId) {
        return findOne("""
                select id, provider_code, event_type, external_event_id, payload::text as payload,
                       signature_valid, processed, received_at
                from payment_webhook_events
                where provider_code = ? and external_event_id = ? and processed = true
                order by received_at desc
                limit 1
                """, statement -> {
            statement.setString(1, providerCode);
            statement.setString(2, externalEventId);
        });
    }

    @Override
    public Optional<PaymentWebhookEvent> findById(UUID id) {
        return findOne("""
                select id, provider_code, event_type, external_event_id, payload::text as payload,
                       signature_valid, processed, received_at
                from payment_webhook_events
                where id = ?
                order by received_at desc
                limit 1
                """, statement -> statement.setObject(1, id));
    }

    private Optional<PaymentWebhookEvent> findOne(String sql, StatementBinder binder) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find payment webhook event", exception);
        }
    }

    private void bind(PreparedStatement statement, PaymentWebhookEvent event) throws SQLException {
        statement.setObject(1, event.id());
        statement.setString(2, event.providerCode());
        statement.setString(3, event.eventType());
        statement.setString(4, event.externalEventId());
        statement.setString(5, event.payloadJson());
        statement.setBoolean(6, event.signatureValid());
        statement.setBoolean(7, event.processed());
        JdbcSupport.setInstant(statement, 8, event.receivedAt());
    }

    private PaymentWebhookEvent map(ResultSet resultSet) throws SQLException {
        Timestamp receivedAt = resultSet.getTimestamp("received_at");
        return new PaymentWebhookEvent(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("provider_code"),
                resultSet.getString("event_type"),
                resultSet.getString("external_event_id"),
                resultSet.getString("payload"),
                resultSet.getBoolean("signature_valid"),
                resultSet.getBoolean("processed"),
                receivedAt.toInstant());
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
