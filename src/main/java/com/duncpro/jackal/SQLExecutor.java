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

    /**
     * This method is identical to {@link #executeQuery(InterpolatedSQLStatement)} except it fetches the query
     * results incrementally instead of eagerly. This method should be preferred when executing queries which
     * could potentially return large datasets. If this {@link SQLExecutor} does not support incremental fetching
     * then an {@link UnsupportedOperationException} is thrown.
     *
     * Unlike the stream returned by {@link #executeQuery(InterpolatedSQLStatement)}, this stream is resourceful
     * and must be closed, either explicitly with {@link Stream#close()} or implicitly with a try-with-resources block.
     * Furthermore any terminal operations performed on this stream may throw {@link UncheckedSQLException}.
     *
     * This returned stream is not inherently synchronous or asynchronous. See the documentation provided by the
     * underlying {@link SQLExecutor} implementation to determine if the returned stream will block or not.
     *
     * The returned stream is lazy, therefore calling this method but never performing a terminal operation on
     * on the returned stream is a no-op.
     */
    protected abstract Stream<QueryResultRow> executeQueryIncrementally(InterpolatedSQLStatement sql);
}
