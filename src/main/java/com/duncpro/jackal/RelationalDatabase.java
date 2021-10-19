package com.duncpro.jackal;

public interface RelationalDatabase extends SQLStatementBuilderFactory {
    RelationalDatabaseTransactionHandle startTransaction() throws RelationalDatabaseException;
}
