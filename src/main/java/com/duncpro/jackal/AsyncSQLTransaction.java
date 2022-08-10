package com.duncpro.jackal;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public abstract class AsyncSQLTransaction extends SQLExecutorProvider {
    public abstract CompletableFuture<Void> commitAsync();

    public abstract CompletableFuture<Void> closeAsync();

    @Override
    protected abstract SQLExecutor getExecutor();
}
