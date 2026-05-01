package com.lottery.infrastructure.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcConnectionProvider {
    Connection currentConnection() throws SQLException;
}
