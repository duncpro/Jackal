package com.duncpro.jackal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.util.Objects.requireNonNull;

public abstract class AsyncSQLTransaction extends SQLExecutorProvider {
    public abstract CompletableFuture<Void> commit();

    public abstract CompletableFuture<Void> close();

    @Override
    protected abstract SQLExecutor getExecutor();
}
