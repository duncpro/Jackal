package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.SQLException;
import com.duncpro.jackal.SQLExecutor;
import com.duncpro.jackal.SQLTransaction;

import java.sql.Connection;
import java.util.concurrent.Executor;

public class JDBCTransaction extends SQLTransaction {
    private final Executor statementExecutor;
    private final Connection connection;
    private volatile boolean isCommitted = false;

    public JDBCTransaction(Executor statementExecutor, Connection connection) {
        this.statementExecutor = statementExecutor;
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
    protected SQLExecutor getExecutor() {
        return new JDBCSQLExecutor(statementExecutor, () -> connection, false);
    }
}
