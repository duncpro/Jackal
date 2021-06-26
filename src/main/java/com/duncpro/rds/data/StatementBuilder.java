package com.duncpro.rds.data;

import javax.annotation.concurrent.NotThreadSafe;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@NotThreadSafe
public interface StatementBuilder {
    StatementBuilder setString(int paramIndex, String arg);
    StatementBuilder setLong(int paramIndex, Long arg);
    StatementBuilder setBoolean(int paramIndex, Boolean arg);

    Stream<QueryResultRow> executeQuery();

    CompletableFuture<Void> executeUpdate();
}
