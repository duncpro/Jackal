package com.duncpro.jackal;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A minimalistic asynchronous SQL API inspired by JDBC.
 */
@ThreadSafe
public abstract class AsyncDatabase implements SQLStatementExecutor {
    protected abstract CompletableFuture<AsyncDatabaseTransaction> startTransactionImpl();

    public final FutureAsyncDatabaseTransaction startTransaction() {
        return new FutureAsyncDatabaseTransaction(startTransactionImpl());
    }

    /**
     * Creates a new transaction and runs the provided transaction procedure asynchronously.
     *
     * All resources allocated as a result of the transaction are freed after the transaction procedure returns.
     *
     * {@link AsyncDatabaseTransaction#commit()} must be explicitly called. A variant of this function
     * {@link #commitTransactionAsync} can be used for implicit committal scenarios.
     * @return a {@link CompletableFuture<T>} encapsulating the value returned by {@code procedure}.
     */
    @Deprecated
    public abstract <T> CompletableFuture<T> runTransactionAsync(Function<AsyncDatabaseTransaction, T> procedure);

    /**
     * Creates a new transaction and executes the provided transaction procedure asynchronously. If an exception is
     * thrown from within the procedure the transaction will be rolled back. If the procedure returns normally without
     * throwing, then the transaction will be committed.
     *
     * All resources allocated as a result of the transaction are freed after the transaction procedure returns.
     * For this reason you should never return a {@link Stream} of values which was produced by the transaction.
     * To mitigate this, just collect the stream via {@link Stream#collect)} before returning.
     *
     * @return a {@link CompletableFuture<T>} encapsulating the value returned by {@code procedure}.
     */
    @Deprecated
    public final  <T> CompletableFuture<T> commitTransactionAsync(Function<AsyncDatabaseTransaction, T> procedure) {
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
