package com.duncpro.jackal.aws;

import com.duncpro.jackal.*;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionResponse;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@ThreadSafe
public class AuroraServerlessDatabase extends SQLDatabase {
    protected final RdsDataAsyncClient rdsDataAsyncClient;
    private final AuroraServerlessCredentials credentials;

    public AuroraServerlessDatabase(final RdsDataAsyncClient rdsDataAsyncClient,
                                    final AuroraServerlessCredentials credentials) {
        this.rdsDataAsyncClient = Objects.requireNonNull(rdsDataAsyncClient);
        this.credentials = Objects.requireNonNull(credentials);
    }

    @Override
    public SQLTransaction startTransaction() throws SQLException {
        final var request = BeginTransactionRequest.builder()
                .resourceArn(this.credentials.dbArn)
                .secretArn(this.credentials.dbSecretArn)
                .build();

        try {
            final var transactionId = this.rdsDataAsyncClient.beginTransaction(request)
                    .thenApply(BeginTransactionResponse::transactionId)
                    .join();

            return new AuroraServerlessTransaction(new AuroraServerlessSQLExecutor(transactionId, rdsDataAsyncClient,
                    credentials));
        } catch (CompletionException e) {
            Throwables.unwrapAndThrow(e, SdkException.class, SQLException::new);
            throw new AssertionError();
        }
    }

    @Override
    public CompletableFuture<AsyncSQLTransaction> startTransactionAsync() {
        final var request = BeginTransactionRequest.builder()
                .resourceArn(this.credentials.dbArn)
                .secretArn(this.credentials.dbSecretArn)
                .build();

        final var awsFuture = this.rdsDataAsyncClient.beginTransaction(request)
                .thenApply(BeginTransactionResponse::transactionId)
                .thenApply(transactionId -> new AuroraServerlessSQLExecutor(transactionId, this.rdsDataAsyncClient,
                        this.credentials))
                .thenApply(AuroraServerlessAsyncTransaction::new);

        final var jackalFuture = new CompletableFuture<AsyncSQLTransaction>();

        awsFuture.whenComplete((transaction, error) -> {
            if (error == null) {
                jackalFuture.complete(transaction);
            } else {
                final var cause = Throwables.unwrapCompletionException(error);
                jackalFuture.completeExceptionally(cause instanceof SdkException ? new SQLException(cause) : cause);
            }
        });

        return jackalFuture;
    }

    @Override
    protected SQLExecutor getExecutor() {
        return new AuroraServerlessSQLExecutor(null, this.rdsDataAsyncClient, this.credentials);
    }
}
