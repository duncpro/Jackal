package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.RelationalDatabaseException;
import com.duncpro.jackal.RelationalDatabaseTransactionHandle;
import com.duncpro.jackal.SQLStatementBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

public class JDBCTransactionHandle implements RelationalDatabaseTransactionHandle {
    private final Connection connection;
    private final boolean originalAutoCommitStatus;
    private boolean isCommitted = false;
    private final ExecutorService executor;

    public JDBCTransactionHandle(ExecutorService executor, Connection connection) throws SQLException {
        this.connection = connection;
        this.originalAutoCommitStatus = this.connection.getAutoCommit();
        this.connection.setAutoCommit(false);
        this.executor = executor;
    }

    @Override
    public void commit() throws RelationalDatabaseException {
        try {
            connection.commit();
            isCommitted = true;
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public void close() throws RelationalDatabaseException {
        try (connection) {
            try {
                if (!isCommitted) {
                    connection.rollback();
                }
            } finally {
                connection.setAutoCommit(originalAutoCommitStatus);
            }
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public SQLStatementBuilder prepareStatement(String parameterizedSQL) {
        return new JDBCStatementBuilder(() -> this.connection, false, executor, parameterizedSQL);
    }
}
