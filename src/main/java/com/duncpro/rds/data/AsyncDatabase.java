package com.duncpro.rds.data;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A minimalistic asynchronous SQL API inspired by JDBC.
 */
public interface AsyncDatabase extends SQLStatementExecutor {
    /**
     * Creates a new transaction and runs the provided transaction procedure asynchronously.
     *
     * All resources allocated as a result of the transaction are freed after the transaction procedure returns.
     *
     * {@link AsyncDatabaseTransaction#commit()} must be explicitly called. A variant of this function
     * {@link #commitTransactionAsync(Function)} can be used for implicit committal scenarios.
     * @return a {@link CompletableFuture<T>} encapsulating the value returned by {@code procedure}.
     */
    <T> CompletableFuture<T> runTransactionAsync(Function<AsyncDatabaseTransaction, T> procedure);

    /**
     * Creates a new transaction and executes the provided transaction procedure asynchronously. If an exception is
     * thrown from within the procedure the transaction will be rolled back. If the procedure returns normally without
     * throwing, then the transaction will be committed.
     *
     * All resources allocated as a result of the transaction are freed after the transaction procedure returns.
     *
     * @return a {@link CompletableFuture<T>} encapsulating the value returned by {@code procedure}.
     */
    default <T> CompletableFuture<T> commitTransactionAsync(Function<AsyncDatabaseTransaction, T> procedure) {
        return runTransactionAsync(transaction -> {
            final T returnValue;
            try {
                returnValue = procedure.apply(transaction);
            } catch (Exception e) {
                transaction.rollback().join();
                throw e;
            }
            transaction.commit().join();
            return returnValue;
        });
    }
}
