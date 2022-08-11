package com.duncpro.jackal;

public abstract class SQLTransaction extends SQLExecutorProvider implements AutoCloseable {
    public abstract void commit() throws SQLException;

    @Override
    public abstract void close() throws SQLException;

    /**
     * Returns a transactional, multi-statement, not-auto-closing {@link SQLExecutor}.
     * Any update statements performed using the returned executor will be committed to the database only after
     * {@link #commit()} is explicitly invoked in application code.
     *
     * The returned {@link SQLExecutor} is not self-closing, and must be explicitly closed after use
     * by calling {@link SQLTransaction#close()} on the {@link SQLTransaction} which produced the {@link SQLExecutor}.
     *
     * This function is marked protected and not intended for consumption in application level code.
     * Instead, use {@link InterpolatableSQLStatement#executeQuery(SQLExecutorProvider)} or
     * {@link InterpolatableSQLStatement#executeUpdate(SQLExecutorProvider)}.
     */
    @Override
    protected abstract SQLExecutor getExecutor();
}
