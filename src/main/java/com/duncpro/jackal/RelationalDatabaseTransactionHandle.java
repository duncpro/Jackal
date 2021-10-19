package com.duncpro.jackal;

public interface RelationalDatabaseTransactionHandle extends AutoCloseable, SQLStatementBuilderFactory {
    void commit() throws RelationalDatabaseException;

    @Override
    public void close() throws RelationalDatabaseException;
}
