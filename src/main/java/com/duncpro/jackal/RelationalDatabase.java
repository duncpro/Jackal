package com.duncpro.jackal;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface RelationalDatabase extends SQLStatementBuilderFactory {
    /**
     * Starts a new transaction. The caller is expected to close the returned {@link TransactionHandle}
     * once they are finished with it.
     */
    TransactionHandle startTransaction() throws RelationalDatabaseException;
}
