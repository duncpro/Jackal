package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.StatementBuilder;
import com.duncpro.jackal.AsyncDatabase;
import com.duncpro.jackal.AsyncDatabaseTransaction;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Implementation of {@link AsyncDatabase} which wraps {@link DataSource}.
 *
 * Warning: Do not use the same {@link ExecutorService} for both {@code transactionExecutor}
 * and {@code sqlExecutor}. By doing so you risk deadlock when using fixed-size thread pools.
 */
@RequiredArgsConstructor
public class DataSourceAsyncWrapper implements AsyncDatabase {
    private final DataSource dataSource;
    private final ExecutorService transactionExecutor;
    private final ExecutorService sqlExecutor;

    private CompletableFuture<Connection> getConnection() {
        return supplyAsync(() -> {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        }, sqlExecutor);
    }

    private void setAutoCommit(Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new AsyncSQLException(e);
        }
    }

    private void close(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new AsyncSQLException(e);
        }
    }

    public <T> CompletableFuture<T> runTransactionAsync(Function<AsyncDatabaseTransaction, T> procedure) {
        return getConnection()
                .thenCompose(connection ->
                        runAsync(() -> setAutoCommit(connection, false), sqlExecutor)
                                .thenApply(($) -> new JdbcTransaction(connection, sqlExecutor))
                                .thenApplyAsync(procedure, transactionExecutor)
                                .whenCompleteAsync(($, $$) -> setAutoCommit(connection, true), sqlExecutor)
                                .whenCompleteAsync(($, $$) -> close(connection), sqlExecutor)
                );
    }

    @Override
    public StatementBuilder prepareStatement(String parameterizedSQL) {
        return new JdbcStatementBuilder(parameterizedSQL, getConnection(), true, sqlExecutor);
    }
}
