package com.duncpro.jackal.aws;

import com.duncpro.jackal.AsyncSQLTransaction;
import com.duncpro.jackal.SQLException;
import com.duncpro.jackal.SQLExecutor;
import software.amazon.awssdk.services.rdsdata.model.CommitTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.RollbackTransactionRequest;

import java.util.concurrent.CompletableFuture;

public class AuroraServerlessAsyncTransaction extends AsyncSQLTransaction  {
    private final AuroraServerlessSQLExecutor executor;

    private volatile boolean isCommitted = false;

    AuroraServerlessAsyncTransaction(AuroraServerlessSQLExecutor executor) {
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Void> commitAsync() {
        final var request = CommitTransactionRequest.builder()
                .resourceArn(executor.credentials.dbArn)
                .secretArn(executor.credentials.dbSecretArn)
                .transactionId(executor.transactionId)
                .build();

        final var awsFuture = this.executor.clients.rdsDataAsyncClient.commitTransaction(request);
        final var jackalFuture = new CompletableFuture<Void>();

        awsFuture.whenComplete((response, error) -> {
            if (error == null) {
                isCommitted = true;
                jackalFuture.complete(null);
            } else {
                jackalFuture.completeExceptionally(new SQLException(error));
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

        final var awsFuture = this.executor.clients.rdsDataAsyncClient.rollbackTransaction(request);
        final var jackalFuture = new CompletableFuture<Void>();

        awsFuture.whenComplete((response, error) -> {
            if (error == null) {
                jackalFuture.complete(null);
            } else {
                jackalFuture.completeExceptionally(new SQLException(error));
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
