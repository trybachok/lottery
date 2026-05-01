package com.lottery.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                  prize_id, is_test, created_at, paid_at, checked_at, cancelled_at, deleted_at, version
                )
                values (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                JdbcSupport.setInstant(statement, 13, ticket.checkedAt().orElse(null));
                JdbcSupport.setInstant(statement, 14, ticket.cancelledAt().orElse(null));
                JdbcSupport.setInstant(statement, 15, ticket.deletedAt().orElse(null));
                statement.setLong(16, ticket.version());
                statement.executeUpdate();
                return ticket;
            }
        } catch (SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Failed to save ticket", exception);
        }
    }

    @Override
    public Optional<Ticket> findById(UUID id) {
        String sql = """
                select id, user_id, draw_id, status, combination_json, price_amount, price_currency, match_percent,
                       prize_id, is_test, created_at, paid_at, checked_at, cancelled_at, deleted_at, version
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

    private Ticket map(ResultSet resultSet) throws SQLException, JsonProcessingException {
        Timestamp paidAt = resultSet.getTimestamp("paid_at");
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
                checkedAt == null ? null : checkedAt.toInstant(),
                cancelledAt == null ? null : cancelledAt.toInstant(),
                deletedAt == null ? null : deletedAt.toInstant(),
                resultSet.getLong("version"));
    }
}
