package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.*;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Implementation of {@link RelationalDatabase} which wraps {@link DataSource}.
 *
 * Warning: Do not use the same {@link ExecutorService} for both {@code transactionExecutor}
 * and {@code sqlExecutor}. By doing so you risk deadlock when using fixed-size thread pools.
 */
@RequiredArgsConstructor
public class DataSourceWrapper implements RelationalDatabase {
    private final DataSource dataSource;
    private final ExecutorService sqlExecutor;

    private CompletableFuture<Connection> getConnection() {
        return supplyAsync(() -> {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                throw new CompletionException(new RelationalDatabaseException(e));
            }
        }, sqlExecutor);
    }

    private void setAutoCommit(Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new CompletionException(new RelationalDatabaseException(e));
        }
    }

    private void close(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new CompletionException(new RelationalDatabaseException(e));
        }
    }

    @Override
    public TransactionHandle startTransaction() throws RelationalDatabaseException {
        final Connection connection;
        try {
            connection = this.dataSource.getConnection();
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }

        return new JdbcTransactionHandle(connection, this.sqlExecutor);
    }

    @Override
    public SQLStatementBuilder prepareStatement(String parameterizedSQL) {
        return new JdbcStatementBuilder(parameterizedSQL, getConnection(), true, sqlExecutor);
    }
}
