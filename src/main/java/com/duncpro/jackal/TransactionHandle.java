package com.duncpro.jackal;

/**
 * Provides support for writing transactions which produce checked exceptions.
 * Since checked exceptions are incompatible with {@link java.util.concurrent.CompletableFuture},
 * this class provides no support for asynchronous commit and rollback operations.
 */
public interface TransactionHandle extends AutoCloseable, SQLStatementBuilderFactory {
    void commit() throws RelationalDatabaseException;

    /**
     * If {@link #commit()} has not been called by the time of closure, the transaction is rolled back.
     * Finally, any resources associated with the transaction are freed.
     */
    @Override
    void close() throws RelationalDatabaseException;
}
