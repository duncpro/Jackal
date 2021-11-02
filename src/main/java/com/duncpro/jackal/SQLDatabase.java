package com.duncpro.jackal;

public abstract class SQLDatabase extends SQLExecutorProvider {
    public abstract SQLTransaction startTransaction() throws SQLException;

    /**
     * Returns an auto-committing sql executor.
     * Any {@link InterpolatableSQLStatement} which is executed by the {@link SQLExecutor} returned by this function
     * will be performed in a single-statement auto-committed transaction.
     */
    @Override
    public abstract SQLExecutor getExecutor();
}
