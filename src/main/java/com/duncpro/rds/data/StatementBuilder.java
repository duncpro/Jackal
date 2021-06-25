package com.duncpro.rds.data;

import javax.annotation.concurrent.NotThreadSafe;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;

@NotThreadSafe
public interface StatementBuilder {
    StatementBuilder setString(int paramIndex, String arg);
    StatementBuilder setLong(int paramIndex, Long arg);
    StatementBuilder setBoolean(int paramIndex, Boolean arg);

    CompletableFuture<QueryResult> executeQuery();

    CompletableFuture<Void> executeUpdate();
}
