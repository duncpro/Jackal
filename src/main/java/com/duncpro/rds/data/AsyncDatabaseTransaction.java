package com.duncpro.rds.data;

import java.util.concurrent.CompletableFuture;

public interface AsyncDatabaseTransaction extends SQLStatementExecutor {
    CompletableFuture<Void> rollback();

    CompletableFuture<Void> commit();
}
