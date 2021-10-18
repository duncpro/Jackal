package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.RelationalDatabaseException;
import com.duncpro.jackal.SQLStatementBuilder;
import com.duncpro.jackal.TransactionHandle;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class JdbcTransactionHandle implements TransactionHandle {
    private final Connection underlyingJdbcConnection;
    private final ExecutorService sqlExecutor;
    boolean isCommitted = false;
    boolean originalAutoCommitStatus;

    public JdbcTransactionHandle(Connection underlyingJdbcConnection, ExecutorService sqlExecutor)
            throws RelationalDatabaseException {
        this.underlyingJdbcConnection = underlyingJdbcConnection;
        this.sqlExecutor = sqlExecutor;

        try {
            this.originalAutoCommitStatus = this.underlyingJdbcConnection.getAutoCommit();
            this.underlyingJdbcConnection.setAutoCommit(false);
        } catch (SQLException e) {
            try {
                this.underlyingJdbcConnection.close();
            } catch (SQLException e2) {
                e.addSuppressed(e2);
            }
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public SQLStatementBuilder prepareStatement(String parameterizedSQL) {
        return new JdbcStatementBuilder(parameterizedSQL, completedFuture(this.underlyingJdbcConnection), false,
                sqlExecutor);
    }

    @Override
    public void commit() throws RelationalDatabaseException {
        try {
            underlyingJdbcConnection.commit();
            isCommitted = true;
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public void close() throws RelationalDatabaseException {
        try (underlyingJdbcConnection) {
            if (!isCommitted) {
                underlyingJdbcConnection.rollback();
            }
            underlyingJdbcConnection.setAutoCommit(originalAutoCommitStatus);
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }
    }
}
