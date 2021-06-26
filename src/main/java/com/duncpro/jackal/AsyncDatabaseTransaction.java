package com.duncpro.jackal;

import java.util.concurrent.CompletableFuture;

public interface AsyncDatabaseTransaction extends SQLStatementExecutor {
    CompletableFuture<Void> rollback();

    CompletableFuture<Void> commit();
}
