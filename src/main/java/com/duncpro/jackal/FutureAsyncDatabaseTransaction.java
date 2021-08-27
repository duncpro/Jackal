package com.duncpro.jackal;

import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;

public class FutureAsyncDatabaseTransaction implements AsyncDatabaseTransaction {
    private final CompletableFuture<AsyncDatabaseTransaction> future;

    FutureAsyncDatabaseTransaction(CompletableFuture<AsyncDatabaseTransaction> future) {
        this.future = future;
    }

    @Override
    public CompletableFuture<Void> rollback() {
        return future.thenCompose(AsyncDatabaseTransaction::rollback);
    }

    @Override
    public CompletableFuture<Void> commit() {
        return future.thenCompose(AsyncDatabaseTransaction::commit);
    }

    @Override
    public StatementBuilder prepareStatement(String parameterizedSQL) {
        final var statementExecutorFuture = future.thenApply(transaction -> (SQLStatementExecutor) transaction);
        return new FutureStatementBuilder(statementExecutorFuture, parameterizedSQL);
    }

    @Override
    public void close() {
        future.thenAccept(transaction -> {
            try {
                transaction.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
