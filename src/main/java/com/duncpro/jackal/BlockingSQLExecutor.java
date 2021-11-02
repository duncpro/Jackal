package com.duncpro.jackal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public abstract class BlockingSQLExecutor extends SQLExecutor {
    private final ExecutorService taskExecutor;

    protected BlockingSQLExecutor(final ExecutorService taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    protected CompletableFuture<Void> executeUpdateAsync(InterpolatedSQLStatement sql) {
        final var future = new CompletableFuture<Void>();
        taskExecutor.submit(() -> {
            try {
                executeUpdate(sql);
                future.complete(null);
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    protected CompletableFuture<Stream<QueryResultRow>> executeQueryAsync(InterpolatedSQLStatement sql) {
        final var future = new CompletableFuture<Stream<QueryResultRow>>();
        taskExecutor.submit(() -> {
            try {
                future.complete(executeQuery(sql));
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
