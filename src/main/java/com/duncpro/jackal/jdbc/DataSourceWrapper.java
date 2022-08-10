package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DataSourceWrapper extends SQLDatabase {
    final Executor statementExecutor;
    final DataSource dataSource;

    public DataSourceWrapper(final Executor statementExecutor, final DataSource dataSource) {
        this.statementExecutor = statementExecutor;
        this.dataSource = dataSource;
    }

    @Override
    public SQLTransaction startTransaction() throws SQLException {
        final Connection connection;
        try {
            connection = dataSource.getConnection();
        }  catch (java.sql.SQLException e) {
            throw new SQLException(e);
        }

        try {
            connection.setAutoCommit(false);
        } catch (java.sql.SQLException e) {
            try {
                connection.close();
            } catch (java.sql.SQLException e1) {
                e.addSuppressed(e1);
            }
            throw new SQLException(e);
        }

        return new JDBCTransaction(statementExecutor, connection);
    }

    @Override
    public CompletableFuture<AsyncSQLTransaction> startTransactionAsync() {
        final var future = new CompletableFuture<AsyncSQLTransaction>();
        statementExecutor.execute(() -> {
            try {
                final var asyncTransaction = wrapBlockingTransaction(this.startTransaction(), statementExecutor);
                future.complete(asyncTransaction);
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private Connection getAutoCommitConnection() throws java.sql.SQLException {
        final var con = dataSource.getConnection();
        try {
            con.setAutoCommit(true);
        } catch (java.sql.SQLException e) {
            try {
                con.close();
            } catch (java.sql.SQLException e1) {
                e.addSuppressed(e1);
            }
            throw e;
        }
        return con;
    }

    @Override
    protected SQLExecutor getExecutor() {
        return new JDBCSQLExecutor(statementExecutor, this::getAutoCommitConnection, true);
    }
}
