package com.duncpro.jackal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class SQLDatabase extends SQLExecutorProvider {
    public abstract SQLTransaction startTransaction() throws SQLException;

    public abstract CompletableFuture<AsyncSQLTransaction> startTransactionAsync();

    /**
     * Returns an auto-committing sql executor.
     * Any {@link InterpolatableSQLStatement} which is executed by this {@link SQLExecutor} will be performed in a
     * single-statement auto-committed transaction.
     */
    @Override
    protected abstract SQLExecutor getExecutor();

    /**
     * Converts a blocking {@link SQLTransaction} to a non-blocking {@link AsyncSQLTransaction} by delegating all
     * blocking calls to the given thread pool. This function is intended to aid in the implementation of
     * {@link SQLDatabase} and is not intended for consumption in application-level code.
     *
     * Use {@link SQLDatabase#startTransactionAsync()} to start a non-blocking transaction in application-level code.
     */
    protected static AsyncSQLTransaction wrapBlockingTransaction(SQLTransaction blockingTransaction, Executor transactionExecutor) {
        return new AsyncSQLTransaction() {
            @Override
            public CompletableFuture<Void> commitAsync() {
                final var future = new CompletableFuture<Void>();
                transactionExecutor.execute(() -> {
                    try {
                        blockingTransaction.commit();
                        future.complete(null);
                    } catch (SQLException e) {
                        future.completeExceptionally(e);
                    }
                });
                return future;
            }

            @Override
            public CompletableFuture<Void> closeAsync() {
                final var future = new CompletableFuture<Void>();
                transactionExecutor.execute(() -> {
                    try {
                        blockingTransaction.close();
                        future.complete(null);
                    } catch (SQLException e) {
                        future.completeExceptionally(e);
                    }
                });
                return future;
            }

            @Override
            protected SQLExecutor getExecutor() {
                return blockingTransaction.getExecutor();
            }
        };
    }
}
