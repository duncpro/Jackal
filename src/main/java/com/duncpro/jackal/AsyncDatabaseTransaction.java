package com.duncpro.jackal;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;

@ThreadSafe
public interface AsyncDatabaseTransaction extends SQLStatementExecutor {
    CompletableFuture<Void> rollback();

    CompletableFuture<Void> commit();
}
