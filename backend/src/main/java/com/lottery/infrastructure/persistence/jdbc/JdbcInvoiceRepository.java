package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.Invoice;
import com.lottery.domain.repository.InvoiceRepository;
import com.lottery.domain.valueobject.InvoiceStatus;
import com.lottery.domain.valueobject.Money;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcInvoiceRepository implements InvoiceRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcInvoiceRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Invoice save(Invoice invoice) {
        String sql = """
                insert into invoices (
                  id, ticket_id, user_id, provider_code, status, amount, currency, external_invoice_id,
                  payment_url, idempotency_key, created_at, expires_at, paid_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindForSave(statement, invoice);
                statement.executeUpdate();
                return invoice;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save invoice", exception);
        }
    }

    @Override
    public Invoice update(Invoice invoice) {
        String sql = """
                update invoices
                set ticket_id = ?, user_id = ?, provider_code = ?, status = ?, amount = ?, currency = ?,
                    external_invoice_id = ?, payment_url = ?, idempotency_key = ?, expires_at = ?, paid_at = ?
                where id = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, invoice.ticketId());
                statement.setObject(2, invoice.userId());
                statement.setString(3, invoice.providerCode());
                statement.setString(4, invoice.status().name());
                statement.setBigDecimal(5, invoice.amount().amount());
                statement.setString(6, invoice.amount().currency().getCurrencyCode());
                statement.setString(7, invoice.maybeExternalInvoiceId().orElse(null));
                statement.setString(8, invoice.maybePaymentUrl().orElse(null));
                statement.setString(9, invoice.idempotencyKey());
                JdbcSupport.setInstant(statement, 10, invoice.maybeExpiresAt().orElse(null));
                JdbcSupport.setInstant(statement, 11, invoice.maybePaidAt().orElse(null));
                statement.setObject(12, invoice.id());
                statement.executeUpdate();
                return invoice;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update invoice", exception);
        }
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return findOne("""
                select id, ticket_id, user_id, provider_code, status, amount, currency, external_invoice_id,
                       payment_url, idempotency_key, created_at, expires_at, paid_at
                from invoices
                where id = ?
                """, statement -> statement.setObject(1, id));
    }

    @Override
    public Optional<Invoice> findByIdempotencyKey(String idempotencyKey) {
        return findOne("""
                select id, ticket_id, user_id, provider_code, status, amount, currency, external_invoice_id,
                       payment_url, idempotency_key, created_at, expires_at, paid_at
                from invoices
                where idempotency_key = ?
                """, statement -> statement.setString(1, idempotencyKey));
    }

    @Override
    public Optional<Invoice> findByExternalInvoiceId(String providerCode, String externalInvoiceId) {
        return findOne("""
                select id, ticket_id, user_id, provider_code, status, amount, currency, external_invoice_id,
                       payment_url, idempotency_key, created_at, expires_at, paid_at
                from invoices
                where provider_code = ? and external_invoice_id = ?
                """, statement -> {
            statement.setString(1, providerCode);
            statement.setString(2, externalInvoiceId);
        });
    }

    @Override
    public Optional<Invoice> findActiveByTicketId(UUID ticketId) {
        return findOne("""
                select id, ticket_id, user_id, provider_code, status, amount, currency, external_invoice_id,
                       payment_url, idempotency_key, created_at, expires_at, paid_at
                from invoices
                where ticket_id = ? and status in ('CREATED', 'PENDING')
                order by created_at desc
                limit 1
                """, statement -> statement.setObject(1, ticketId));
    }

    @Override
    public List<Invoice> findByTicketId(UUID ticketId) {
        String sql = """
                select id, ticket_id, user_id, provider_code, status, amount, currency, external_invoice_id,
                       payment_url, idempotency_key, created_at, expires_at, paid_at
                from invoices
                where ticket_id = ?
                order by created_at desc
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, ticketId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Invoice> invoices = new ArrayList<>();
                    while (resultSet.next()) {
                        invoices.add(map(resultSet));
                    }
                    return invoices;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list ticket invoices", exception);
        }
    }

    @Override
    public List<Invoice> findByUserId(UUID userId, int limit, int offset) {
        String sql = """
                select id, ticket_id, user_id, provider_code, status, amount, currency, external_invoice_id,
                       payment_url, idempotency_key, created_at, expires_at, paid_at
                from invoices
                where user_id = ?
                order by created_at desc
                limit ? offset ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, userId);
                statement.setInt(2, limit);
                statement.setInt(3, offset);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Invoice> invoices = new ArrayList<>();
                    while (resultSet.next()) {
                        invoices.add(map(resultSet));
                    }
                    return invoices;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list invoices", exception);
        }
    }

    private Optional<Invoice> findOne(String sql, StatementBinder binder) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find invoice", exception);
        }
    }

    private void bindForSave(PreparedStatement statement, Invoice invoice) throws SQLException {
        statement.setObject(1, invoice.id());
        statement.setObject(2, invoice.ticketId());
        statement.setObject(3, invoice.userId());
        statement.setString(4, invoice.providerCode());
        statement.setString(5, invoice.status().name());
        statement.setBigDecimal(6, invoice.amount().amount());
        statement.setString(7, invoice.amount().currency().getCurrencyCode());
        statement.setString(8, invoice.maybeExternalInvoiceId().orElse(null));
        statement.setString(9, invoice.maybePaymentUrl().orElse(null));
        statement.setString(10, invoice.idempotencyKey());
        JdbcSupport.setInstant(statement, 11, invoice.createdAt());
        JdbcSupport.setInstant(statement, 12, invoice.maybeExpiresAt().orElse(null));
        JdbcSupport.setInstant(statement, 13, invoice.maybePaidAt().orElse(null));
    }

    private Invoice map(ResultSet resultSet) throws SQLException {
        Timestamp expiresAt = resultSet.getTimestamp("expires_at");
        Timestamp paidAt = resultSet.getTimestamp("paid_at");
        return new Invoice(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("ticket_id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("provider_code"),
                InvoiceStatus.valueOf(resultSet.getString("status")),
                new Money(resultSet.getBigDecimal("amount"), Currency.getInstance(resultSet.getString("currency"))),
                resultSet.getString("external_invoice_id"),
                resultSet.getString("payment_url"),
                resultSet.getString("idempotency_key"),
                resultSet.getTimestamp("created_at").toInstant(),
                expiresAt == null ? null : expiresAt.toInstant(),
                paidAt == null ? null : paidAt.toInstant());
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
