package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.Prize;
import com.lottery.domain.repository.PrizeRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcPrizeRepository implements PrizeRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcPrizeRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public List<Prize> findAll(int limit, int offset) {
        String sql = """
                select id, type, name, amount, currency, product_id, quantity, unit
                from prizes
                order by name asc, id asc
                limit ? offset ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Prize> prizes = new ArrayList<>();
                    while (resultSet.next()) {
                        prizes.add(map(resultSet));
                    }
                    return prizes;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list prizes", exception);
        }
    }

    @Override
    public Optional<Prize> findById(UUID id) {
        String sql = """
                select id, type, name, amount, currency, product_id, quantity, unit
                from prizes
                where id = ?
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
            throw new IllegalStateException("Failed to find prize", exception);
        }
    }

    @Override
    public Prize save(Prize prize) {
        String sql = """
                insert into prizes (id, type, name, amount, currency, product_id, quantity, unit)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, prize);
                statement.executeUpdate();
                return prize;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save prize", exception);
        }
    }

    @Override
    public Prize update(Prize prize) {
        String sql = """
                update prizes
                set type = ?, name = ?, amount = ?, currency = ?, product_id = ?, quantity = ?, unit = ?
                where id = ?
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, prize.type());
                statement.setString(2, prize.name());
                statement.setBigDecimal(3, prize.amount());
                statement.setString(4, prize.currency() == null ? null : prize.currency().getCurrencyCode());
                statement.setObject(5, prize.productId());
                statement.setBigDecimal(6, prize.quantity());
                statement.setString(7, prize.unit());
                statement.setObject(8, prize.id());
                statement.executeUpdate();
                return prize;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update prize", exception);
        }
    }

    private void bind(PreparedStatement statement, Prize prize) throws SQLException {
        statement.setObject(1, prize.id());
        statement.setString(2, prize.type());
        statement.setString(3, prize.name());
        statement.setBigDecimal(4, prize.amount());
        statement.setString(5, prize.currency() == null ? null : prize.currency().getCurrencyCode());
        statement.setObject(6, prize.productId());
        statement.setBigDecimal(7, prize.quantity());
        statement.setString(8, prize.unit());
    }

    private Prize map(ResultSet resultSet) throws SQLException {
        String currencyCode = resultSet.getString("currency");
        return new Prize(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("type"),
                resultSet.getString("name"),
                resultSet.getBigDecimal("amount"),
                currencyCode == null ? null : Currency.getInstance(currencyCode),
                resultSet.getObject("product_id", UUID.class),
                resultSet.getBigDecimal("quantity"),
                resultSet.getString("unit"));
    }
}
