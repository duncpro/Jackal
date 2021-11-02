package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.SQLException;
import com.duncpro.jackal.SQLExecutor;
import com.duncpro.jackal.SQLTransaction;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public class JDBCTransaction extends SQLTransaction {
    private final ExecutorService taskExecutor;
    private final Connection connection;
    private volatile boolean isCommitted = false;

    public JDBCTransaction(ExecutorService taskExecutor, Connection connection) {
        this.taskExecutor = taskExecutor;
        this.connection = connection;
    }

    @Override
    public void commit() throws SQLException {
        try {
            connection.commit();
            isCommitted = true;
        } catch (java.sql.SQLException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void close() throws SQLException {
        try (connection) {
            if (!isCommitted) {
                connection.rollback();
            }
        } catch (java.sql.SQLException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public SQLExecutor getExecutor() {
        return new JDBCSQLExecutor(taskExecutor, () -> connection, false);
    }
}
