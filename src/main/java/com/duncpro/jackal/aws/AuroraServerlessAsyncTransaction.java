package com.duncpro.jackal.aws;

import com.duncpro.jackal.AsyncSQLTransaction;
import com.duncpro.jackal.SQLException;
import com.duncpro.jackal.SQLExecutor;
import com.duncpro.jackal.Throwables;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.model.CommitTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.RollbackTransactionRequest;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class AuroraServerlessAsyncTransaction extends AsyncSQLTransaction  {
    private final AuroraServerlessSQLExecutor executor;

    private volatile boolean isCommitted = false;

    AuroraServerlessAsyncTransaction(AuroraServerlessSQLExecutor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public CompletableFuture<Void> commitAsync() {
        final var request = CommitTransactionRequest.builder()
                .resourceArn(executor.credentials.dbArn)
                .secretArn(executor.credentials.dbSecretArn)
                .transactionId(executor.transactionId)
                .build();

        final var awsFuture = this.executor.rdsDataAsyncClient.commitTransaction(request);
        final var jackalFuture = new CompletableFuture<Void>();

        awsFuture.whenComplete((response, error) -> {
            if (error == null) {
                isCommitted = true;
                jackalFuture.complete(null);
            } else {
                final var cause = Throwables.unwrapCompletionException(error);
                jackalFuture.completeExceptionally(cause instanceof SdkException ? new SQLException(cause) : cause);
            }
        });

        return jackalFuture;
    }

    private CompletableFuture<Void> rollback() {
        final var request = RollbackTransactionRequest.builder()
                .resourceArn(this.executor.credentials.dbArn)
                .secretArn(this.executor.credentials.dbSecretArn)
                .transactionId(this.executor.transactionId)
                .build();

        final var awsFuture = this.executor.rdsDataAsyncClient.rollbackTransaction(request);
        final var jackalFuture = new CompletableFuture<Void>();

        awsFuture.whenComplete((response, error) -> {
            if (error == null) {
                jackalFuture.complete(null);
            } else {
                final var cause = Throwables.unwrapCompletionException(error);
                jackalFuture.completeExceptionally(cause instanceof SdkException ? new SQLException(cause) : cause);
            }
        });

        return jackalFuture;
    }


    @Override
    public CompletableFuture<Void> closeAsync() {
        if (isCommitted) return CompletableFuture.completedFuture(null);
        return rollback();
    }

    @Override
    protected SQLExecutor getExecutor() {
        return this.executor;
    }
}
