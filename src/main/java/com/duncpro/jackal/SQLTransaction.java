package com.duncpro.jackal;

public abstract class SQLTransaction extends SQLExecutorProvider implements AutoCloseable {
    public abstract void commit() throws SQLException;

    @Override
    public abstract void close() throws SQLException;

    /**
     * Returns a transactional {@link SQLExecutor}. Any update statements performed using the
     * returned executor will only be committed to the database after {@link #commit()} is invoked.
     */
    @Override
    public abstract SQLExecutor getExecutor();
}
