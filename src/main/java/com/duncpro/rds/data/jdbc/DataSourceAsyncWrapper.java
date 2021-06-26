package com.duncpro.rds.data.jdbc;

import com.duncpro.rds.data.AsyncDatabase;
import com.duncpro.rds.data.AsyncDatabaseTransaction;
import com.duncpro.rds.data.StatementBuilder;
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

    private CompletableFuture<Void> setAutoCommit(Connection connection, boolean autoCommit) {
        return runAsync(() -> {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        }, sqlExecutor);
    }

    private CompletableFuture<Void> close(Connection connection) {
        return runAsync(() -> {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        }, transactionExecutor);
    }

    public <T> CompletableFuture<T> runTransactionAsync(Function<AsyncDatabaseTransaction, T> procedure) {
        return getConnection()
                .thenCompose(connection ->
                        setAutoCommit(connection, false)
                                .thenApply(($) -> new JdbcTransaction(connection, sqlExecutor))
                                .thenApplyAsync(procedure, transactionExecutor)
                                .whenCompleteAsync(($, $$) -> setAutoCommit(connection, true).join())
                                .whenCompleteAsync(($, $$) -> close(connection).join())
                );
    }

    @Override
    public StatementBuilder prepareStatement(String parameterizedSQL) {
        return new JdbcStatementBuilder(parameterizedSQL, getConnection(), true, sqlExecutor);
    }
}
