package com.duncpro.jackal;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface SQLStatementBuilder {
    SQLStatementBuilder withArguments(Object... args);

    CompletableFuture<Void> executeUpdateAsync();

    void executeUpdate() throws RelationalDatabaseException;

    CompletableFuture<Stream<QueryResultRow>> executeQueryAsync();

    Stream<QueryResultRow> executeQuery() throws RelationalDatabaseException;
}
