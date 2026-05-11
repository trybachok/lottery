package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.domain.model.WinningRule;
import com.lottery.domain.repository.WinningRuleRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class JdbcWinningRuleRepository implements WinningRuleRepository {
    private final JdbcConnectionProvider connectionProvider;

    public JdbcWinningRuleRepository(JdbcConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public List<WinningRule> findByDrawIdOrderByPriority(UUID drawId) {
        String sql = """
                select id, draw_id, match_percent_from, match_percent_to, prize_id, priority
                from winning_rules
                where draw_id = ?
                order by priority asc
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, drawId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<WinningRule> rules = new ArrayList<>();
                    while (resultSet.next()) {
                        rules.add(new WinningRule(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getObject("draw_id", UUID.class),
                                resultSet.getBigDecimal("match_percent_from"),
                                resultSet.getBigDecimal("match_percent_to"),
                                resultSet.getObject("prize_id", UUID.class),
                                resultSet.getInt("priority")));
                    }
                    return rules;
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list winning rules", exception);
        }
    }

    @Override
    public WinningRule save(WinningRule winningRule) {
        String sql = """
                insert into winning_rules (id, draw_id, match_percent_from, match_percent_to, prize_id, priority)
                values (?, ?, ?, ?, ?, ?)
                """;
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, winningRule.id());
                statement.setObject(2, winningRule.drawId());
                statement.setBigDecimal(3, winningRule.matchPercentFrom());
                statement.setBigDecimal(4, winningRule.matchPercentTo());
                statement.setObject(5, winningRule.prizeId());
                statement.setInt(6, winningRule.priority());
                statement.executeUpdate();
                return winningRule;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save winning rule", exception);
        }
    }

    @Override
    public void deleteByDrawId(UUID drawId) {
        String sql = "delete from winning_rules where draw_id = ?";
        try {
            Connection connection = connectionProvider.currentConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, drawId);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete winning rules", exception);
        }
    }
}
