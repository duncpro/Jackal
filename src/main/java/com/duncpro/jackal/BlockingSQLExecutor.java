package com.duncpro.jackal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public abstract class BlockingSQLExecutor extends SQLExecutor {
    private final Executor statementExecutor;

    protected BlockingSQLExecutor(final Executor statementExecutor) {
        this.statementExecutor = statementExecutor;
    }

    @Override
    protected CompletableFuture<Void> executeUpdateAsync(InterpolatedSQLStatement sql) {
        final var future = new CompletableFuture<Void>();
        statementExecutor.execute(() -> {
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
        statementExecutor.execute(() -> {
            try {
                future.complete(executeQuery(sql));
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    /**
     * The returned stream is not asynchronous, any terminal operations performed on the stream
     * will block.
     */
    @Override
    protected abstract Stream<QueryResultRow> executeQueryIncrementally(InterpolatedSQLStatement sql);
}
