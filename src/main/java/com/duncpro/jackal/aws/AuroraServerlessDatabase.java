package com.duncpro.jackal.aws;

import com.duncpro.jackal.*;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionResponse;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;

@ThreadSafe
public class AuroraServerlessDatabase extends SQLDatabase {
    protected final AuroraServerlessClientBundle clients;
    private final AuroraServerlessCredentials credentials;

    public AuroraServerlessDatabase(final AuroraServerlessClientBundle clients,
                                    final AuroraServerlessCredentials credentials) {
        this.clients = clients;
        this.credentials = credentials;
    }

    @Override
    public SQLTransaction startTransaction() throws SQLException {
        final var request = BeginTransactionRequest.builder()
                .resourceArn(this.credentials.dbArn)
                .secretArn(this.credentials.dbSecretArn)
                .build();

        try {
            final var transactionId = this.clients.rdsDataClient.beginTransaction(request).transactionId();
            return new AuroraServerlessTransaction(new AuroraServerlessSQLExecutor(transactionId, this.clients,
                    this.credentials));
        } catch (SdkException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public CompletableFuture<AsyncSQLTransaction> startTransactionAsync() {
        final var request = BeginTransactionRequest.builder()
                .resourceArn(this.credentials.dbArn)
                .secretArn(this.credentials.dbSecretArn)
                .build();

        final var awsFuture = this.clients.rdsDataAsyncClient.beginTransaction(request)
                .thenApply(BeginTransactionResponse::transactionId)
                .thenApply(transactionId -> new AuroraServerlessSQLExecutor(transactionId, this.clients,
                        this.credentials))
                .thenApply(AuroraServerlessAsyncTransaction::new);

        final var jackalFuture = new CompletableFuture<AsyncSQLTransaction>();

        awsFuture.whenComplete((transaction, error) -> {
            if (error == null) {
                jackalFuture.completeExceptionally(new SQLException(error));
            } else {
                jackalFuture.complete(transaction);
            }
        });

        return jackalFuture;
    }

    @Override
    protected SQLExecutor getExecutor() {
        return new AuroraServerlessSQLExecutor(null, this.clients, this.credentials);
    }
}
