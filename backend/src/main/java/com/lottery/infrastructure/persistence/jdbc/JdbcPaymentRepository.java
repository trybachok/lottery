package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.Payment;
import com.lottery.domain.repository.PaymentRepository;
import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.PaymentStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcPaymentRepository implements PaymentRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcPaymentRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Payment save(Payment payment) {
        String sql = """
                insert into payments (id, invoice_id, provider_code, status, amount, currency, external_payment_id, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindForSave(statement, payment);
                statement.executeUpdate();
                return payment;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save payment", exception);
        }
    }

    @Override
    public Payment update(Payment payment) {
        String sql = """
                update payments
                set invoice_id = ?, provider_code = ?, status = ?, amount = ?, currency = ?, external_payment_id = ?, updated_at = ?
                where id = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, payment.invoiceId());
                statement.setString(2, payment.providerCode());
                statement.setString(3, payment.status().name());
                statement.setBigDecimal(4, payment.amount().amount());
                statement.setString(5, payment.amount().currency().getCurrencyCode());
                statement.setString(6, payment.maybeExternalPaymentId().orElse(null));
                JdbcSupport.setInstant(statement, 7, payment.updatedAt());
                statement.setObject(8, payment.id());
                statement.executeUpdate();
                return payment;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update payment", exception);
        }
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return findOne("""
                select id, invoice_id, provider_code, status, amount, currency, external_payment_id, created_at, updated_at
                from payments
                where id = ?
                """, statement -> statement.setObject(1, id));
    }

    @Override
    public Optional<Payment> findByExternalPaymentId(String externalPaymentId) {
        return findOne("""
                select id, invoice_id, provider_code, status, amount, currency, external_payment_id, created_at, updated_at
                from payments
                where external_payment_id = ?
                """, statement -> statement.setString(1, externalPaymentId));
    }

    @Override
    public List<Payment> findByInvoiceId(UUID invoiceId, int limit, int offset) {
        String sql = """
                select id, invoice_id, provider_code, status, amount, currency, external_payment_id, created_at, updated_at
                from payments
                where invoice_id = ?
                order by created_at desc
                limit ? offset ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, invoiceId);
                statement.setInt(2, limit);
                statement.setInt(3, offset);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Payment> payments = new ArrayList<>();
                    while (resultSet.next()) {
                        payments.add(map(resultSet));
                    }
                    return payments;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list payments", exception);
        }
    }

    private Optional<Payment> findOne(String sql, StatementBinder binder) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find payment", exception);
        }
    }

    private void bindForSave(PreparedStatement statement, Payment payment) throws SQLException {
        statement.setObject(1, payment.id());
        statement.setObject(2, payment.invoiceId());
        statement.setString(3, payment.providerCode());
        statement.setString(4, payment.status().name());
        statement.setBigDecimal(5, payment.amount().amount());
        statement.setString(6, payment.amount().currency().getCurrencyCode());
        statement.setString(7, payment.maybeExternalPaymentId().orElse(null));
        JdbcSupport.setInstant(statement, 8, payment.createdAt());
        JdbcSupport.setInstant(statement, 9, payment.updatedAt());
    }

    private Payment map(ResultSet resultSet) throws SQLException {
        return new Payment(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("invoice_id", UUID.class),
                resultSet.getString("provider_code"),
                PaymentStatus.valueOf(resultSet.getString("status")),
                new Money(resultSet.getBigDecimal("amount"), Currency.getInstance(resultSet.getString("currency"))),
                resultSet.getString("external_payment_id"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
