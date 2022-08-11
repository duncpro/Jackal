package com.duncpro.jackal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class SQLDatabase extends SQLExecutorProvider {
    public abstract SQLTransaction startTransaction() throws SQLException;

    public abstract CompletableFuture<AsyncSQLTransaction> startTransactionAsync();

    /**
     * Returns a new, single-statement, auto-committing, auto-closing {@link SQLExecutor}.
     * It may be possible to execute multiple statements when using some implementations of
     * {@link SQLDatabase}, but such an action should never be undertaken, since the {@link SQLExecutor}
     * can auto-close at any time after at least one statement is executed. To mitigate this foot-gun,
     * this method is explicitly marked as protected. This function should never be used in application-level
     * code. Instead {@link InterpolatableSQLStatement#executeQuery(SQLExecutorProvider)} should be used instead.
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
