package com.duncpro.jackal;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@ThreadSafe
public abstract class SQLExecutor {
    protected abstract CompletableFuture<Void> executeUpdateAsync(InterpolatedSQLStatement sql);
    protected abstract void executeUpdate(InterpolatedSQLStatement sql) throws SQLException;

    protected abstract CompletableFuture<Stream<QueryResultRow>> executeQueryAsync(InterpolatedSQLStatement sql);
    protected abstract Stream<QueryResultRow> executeQuery(InterpolatedSQLStatement sql) throws SQLException;
}
