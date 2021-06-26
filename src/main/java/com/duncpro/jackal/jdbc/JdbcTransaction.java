package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.StatementBuilder;
import com.duncpro.jackal.AsyncDatabaseTransaction;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.runAsync;

@RequiredArgsConstructor
class JdbcTransaction implements AsyncDatabaseTransaction {
    private final Connection connection;
    private final ExecutorService executor;

    @Override
    public CompletableFuture<Void> rollback() {
        return runAsync(() -> {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> commit() {
        return runAsync(() -> {
            try {
                connection.commit();
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
