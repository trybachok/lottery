package com.lottery.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.application.OptimisticLockException;
import com.lottery.domain.model.Ticket;
import com.lottery.domain.repository.TicketRepository;
import com.lottery.domain.valueobject.Combination;
import com.lottery.domain.valueobject.Money;
import com.lottery.domain.valueobject.TicketStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcTicketRepository implements TicketRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcConnectionProvider connectionProvider;
    private final ObjectMapper objectMapper;

    public JdbcTicketRepository(JdbcConnectionProvider connectionProvider, ObjectMapper objectMapper) {
        this.connectionProvider = connectionProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public Ticket save(Ticket ticket) {
        String sql = """
                insert into tickets (
                  id, user_id, draw_id, status, combination_json, price_amount, price_currency, match_percent,
                  prize_id, is_test, created_at, paid_at, participated_at, checked_at, cancelled_at, deleted_at, version
                )
                values (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, ticket.id());
                statement.setObject(2, ticket.userId());
                statement.setObject(3, ticket.drawId());
                statement.setString(4, ticket.status().name());
                statement.setString(5, objectMapper.writeValueAsString(ticket.combination().values()));
                statement.setBigDecimal(6, ticket.price().amount());
                statement.setString(7, ticket.price().currency().getCurrencyCode());
                statement.setBigDecimal(8, ticket.matchPercent().orElse(null));
                statement.setObject(9, ticket.prizeId().orElse(null));
                statement.setBoolean(10, ticket.test());
                JdbcSupport.setInstant(statement, 11, ticket.createdAt());
                JdbcSupport.setInstant(statement, 12, ticket.paidAt().orElse(null));
                JdbcSupport.setInstant(statement, 13, ticket.participatedAt().orElse(null));
                JdbcSupport.setInstant(statement, 14, ticket.checkedAt().orElse(null));
                JdbcSupport.setInstant(statement, 15, ticket.cancelledAt().orElse(null));
                JdbcSupport.setInstant(statement, 16, ticket.deletedAt().orElse(null));
                statement.setLong(17, ticket.version());
                statement.executeUpdate();
                return ticket;
            }
        } catch (SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Failed to save ticket", exception);
        }
    }

    @Override
    public Ticket update(Ticket ticket) {
        String sql = """
                update tickets
                set user_id = ?, draw_id = ?, status = ?, combination_json = ?::jsonb, price_amount = ?,
                    price_currency = ?, match_percent = ?, prize_id = ?, is_test = ?, paid_at = ?,
                    participated_at = ?, checked_at = ?, cancelled_at = ?, deleted_at = ?, version = version + 1
                where id = ? and version = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, ticket.userId());
                statement.setObject(2, ticket.drawId());
                statement.setString(3, ticket.status().name());
                statement.setString(4, objectMapper.writeValueAsString(ticket.combination().values()));
                statement.setBigDecimal(5, ticket.price().amount());
                statement.setString(6, ticket.price().currency().getCurrencyCode());
                statement.setBigDecimal(7, ticket.matchPercent().orElse(null));
                statement.setObject(8, ticket.prizeId().orElse(null));
                statement.setBoolean(9, ticket.test());
                JdbcSupport.setInstant(statement, 10, ticket.paidAt().orElse(null));
                JdbcSupport.setInstant(statement, 11, ticket.participatedAt().orElse(null));
                JdbcSupport.setInstant(statement, 12, ticket.checkedAt().orElse(null));
                JdbcSupport.setInstant(statement, 13, ticket.cancelledAt().orElse(null));
                JdbcSupport.setInstant(statement, 14, ticket.deletedAt().orElse(null));
                statement.setObject(15, ticket.id());
                statement.setLong(16, ticket.version());
                int updated = statement.executeUpdate();
                if (updated != 1) {
                    throw new OptimisticLockException("Ticket");
                }
                return new Ticket(
                        ticket.id(),
                        ticket.userId(),
                        ticket.drawId(),
                        ticket.status(),
                        ticket.combination(),
                        ticket.price(),
                        ticket.matchPercent().orElse(null),
                        ticket.prizeId().orElse(null),
                        ticket.test(),
                        ticket.createdAt(),
                        ticket.paidAt().orElse(null),
                        ticket.participatedAt().orElse(null),
                        ticket.checkedAt().orElse(null),
                        ticket.cancelledAt().orElse(null),
                        ticket.deletedAt().orElse(null),
                        ticket.version() + 1);
            }
        } catch (SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Failed to update ticket", exception);
        }
    }

    @Override
    public Optional<Ticket> findById(UUID id) {
        String sql = """
                select id, user_id, draw_id, status, combination_json, price_amount, price_currency, match_percent,
                       prize_id, is_test, created_at, paid_at, participated_at, checked_at, cancelled_at, deleted_at, version
                from tickets
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
        } catch (SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Failed to find ticket", exception);
        }
    }

    @Override
    public List<Ticket> findAll(int limit, int offset) {
        return findMany("""
                select id, user_id, draw_id, status, combination_json, price_amount, price_currency, match_percent,
                       prize_id, is_test, created_at, paid_at, participated_at, checked_at, cancelled_at, deleted_at, version
                from tickets
                where deleted_at is null
                order by created_at desc
                limit ? offset ?
                """, statement -> {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
        });
    }

    @Override
    public List<Ticket> findByUserId(UUID userId, int limit, int offset) {
        return findMany("""
                select id, user_id, draw_id, status, combination_json, price_amount, price_currency, match_percent,
                       prize_id, is_test, created_at, paid_at, participated_at, checked_at, cancelled_at, deleted_at, version
                from tickets
                where user_id = ? and deleted_at is null
                order by created_at desc
                limit ? offset ?
                """, statement -> {
            statement.setObject(1, userId);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
        });
    }

    @Override
    public List<Ticket> findPaidByDrawId(UUID drawId) {
        return findMany("""
                select id, user_id, draw_id, status, combination_json, price_amount, price_currency, match_percent,
                       prize_id, is_test, created_at, paid_at, participated_at, checked_at, cancelled_at, deleted_at, version
                from tickets
                where draw_id = ? and status = 'PAID' and cancelled_at is null and deleted_at is null
                order by created_at asc
                for update
                """, statement -> statement.setObject(1, drawId));
    }

    @Override
    public long countActiveByDrawId(UUID drawId) {
        String sql = """
                select count(*)
                from tickets
                where draw_id = ? and deleted_at is null and status <> 'DELETED'
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, drawId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getLong(1);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count draw tickets", exception);
        }
    }

    @Override
    public List<Ticket> findReport(
            UUID userId,
            UUID drawId,
            TicketStatus status,
            Instant createdFrom,
            Instant createdTo,
            int limit,
            int offset) {
        StringBuilder sql = new StringBuilder("""
                select id, user_id, draw_id, status, combination_json, price_amount, price_currency, match_percent,
                       prize_id, is_test, created_at, paid_at, participated_at, checked_at, cancelled_at, deleted_at, version
                from tickets
                where deleted_at is null
                """);
        List<SqlParameter> parameters = new ArrayList<>();
        if (userId != null) {
            sql.append(" and user_id = ?");
            parameters.add((statement, index) -> statement.setObject(index, userId));
        }
        if (drawId != null) {
            sql.append(" and draw_id = ?");
            parameters.add((statement, index) -> statement.setObject(index, drawId));
        }
        if (status != null) {
            sql.append(" and status = ?");
            parameters.add((statement, index) -> statement.setString(index, status.name()));
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
        return findMany(sql.toString(), statement -> {
            int index = 1;
            for (SqlParameter parameter : parameters) {
                parameter.bind(statement, index++);
            }
            statement.setInt(index++, limit);
            statement.setInt(index, offset);
        });
    }

    private List<Ticket> findMany(String sql, StatementBinder binder) {
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Ticket> tickets = new ArrayList<>();
                    while (resultSet.next()) {
                        tickets.add(map(resultSet));
                    }
                    return tickets;
                }
            }
        } catch (SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Failed to list tickets", exception);
        }
    }

    private Ticket map(ResultSet resultSet) throws SQLException, JsonProcessingException {
        Timestamp paidAt = resultSet.getTimestamp("paid_at");
        Timestamp participatedAt = resultSet.getTimestamp("participated_at");
        Timestamp checkedAt = resultSet.getTimestamp("checked_at");
        Timestamp cancelledAt = resultSet.getTimestamp("cancelled_at");
        Timestamp deletedAt = resultSet.getTimestamp("deleted_at");
        List<String> values = objectMapper.readValue(resultSet.getString("combination_json"), STRING_LIST);
        return new Ticket(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getObject("draw_id", UUID.class),
                TicketStatus.valueOf(resultSet.getString("status")),
                new Combination(values),
                new Money(resultSet.getBigDecimal("price_amount"), Currency.getInstance(resultSet.getString("price_currency"))),
                resultSet.getBigDecimal("match_percent"),
                resultSet.getObject("prize_id", UUID.class),
                resultSet.getBoolean("is_test"),
                resultSet.getTimestamp("created_at").toInstant(),
                paidAt == null ? null : paidAt.toInstant(),
                participatedAt == null ? null : participatedAt.toInstant(),
                checkedAt == null ? null : checkedAt.toInstant(),
                cancelledAt == null ? null : cancelledAt.toInstant(),
                deletedAt == null ? null : deletedAt.toInstant(),
                resultSet.getLong("version"));
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    private interface SqlParameter {
        void bind(PreparedStatement statement, int index) throws SQLException;
    }
}
