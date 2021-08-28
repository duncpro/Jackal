package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.StatementBuilder;
import com.duncpro.jackal.AsyncDatabaseTransaction;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.runAsync;

@RequiredArgsConstructor
class JdbcTransaction implements AsyncDatabaseTransaction {
    private final Connection connection;
    private final ExecutorService executor;

    @Override
    public CompletableFuture<Void> rollback() {
        final Runnable rollbacker = () -> {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        };

        return runAsync(rollbacker, executor)
                .thenCompose($ -> this.close());
    }

    @Override
    public CompletableFuture<Void> commit() {
        final Runnable commiter = () -> {
            try {
                connection.commit();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        };

        return runAsync(commiter, executor)
                .thenCompose($ -> this.close());
    }

    private CompletableFuture<Void> close() {
        return runAsync(() -> {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        }, executor);
    }

    @Override
    public StatementBuilder prepareStatement(String parameterizedSQL) {
        return new JdbcStatementBuilder(parameterizedSQL, CompletableFuture.completedFuture(connection),
                false, executor);
    }
}
