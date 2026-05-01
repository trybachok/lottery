package com.lottery.infrastructure.persistence.jdbc;

import com.lottery.application.port.transaction.TransactionManager;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public final class JdbcTransactionManager implements TransactionManager, JdbcConnectionProvider {
    private final DataSource dataSource;
    private final ThreadLocal<Connection> currentConnection = new ThreadLocal<>();

    public JdbcTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T inTransaction(TransactionalWork<T> work) {
        if (currentConnection.get() != null) {
            return work.execute();
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            currentConnection.set(connection);
            try {
                T result = work.execute();
                connection.commit();
                return result;
            } catch (RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                currentConnection.remove();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Database transaction failed", exception);
        }
    }

    @Override
    public Connection currentConnection() throws SQLException {
        Connection connection = currentConnection.get();
        if (connection == null) {
            throw new SQLException("No active JDBC transaction");
        }
        return connection;
    }
}
