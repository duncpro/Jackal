package com.duncpro.jackal.rds;

import com.duncpro.jackal.StatementBuilder;
import com.duncpro.jackal.AsyncDatabase;
import com.duncpro.jackal.AsyncDatabaseTransaction;
import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionResponse;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Exposes {@link RdsDataClient} as an {@link AsyncDatabase}.
 */
@ThreadSafe
public class AmazonDataAPIDatabase implements AsyncDatabase {
    final RdsDataAsyncClient rdsDataClient;
    final String databaseArn;
    final String databaseSecretArn;
    final ExecutorService transactionExecutor;

    /**
     * @param rdsDataClient the {@link RdsDataAsyncClient} client which will be used to communicate with AWS.
     * @param databaseArn the ARN of the AWS RDS Database Cluster to connect to.
     * @param databaseSecretArn the ARN of the AWS SecretsManager Secret which contains the credentials for this database.
     * @param transactionExecutor the {@link ExecutorService} which all transaction tasks will be run on.
     */
    public AmazonDataAPIDatabase(RdsDataAsyncClient rdsDataClient, String databaseArn,
                                 String databaseSecretArn, ExecutorService transactionExecutor) {
        this.rdsDataClient = rdsDataClient;
        this.databaseArn = databaseArn;
        this.databaseSecretArn = databaseSecretArn;
        this.transactionExecutor = transactionExecutor;
    }

    private CompletableFuture<AmazonDataAPITransaction> startTransaction() {
        final var request = BeginTransactionRequest.builder()
                .secretArn(databaseSecretArn)
                .resourceArn(databaseArn)
                .build();

        return rdsDataClient.beginTransaction(request)
                .thenApply(BeginTransactionResponse::transactionId)
                .thenApply(transactionId -> new AmazonDataAPITransaction(this, transactionId));
    }

    @Override
    public <T> CompletableFuture<T> runTransactionAsync(Function<AsyncDatabaseTransaction, T> procedure) {
        return startTransaction()
                .thenCompose(transaction ->
                        supplyAsync(() -> procedure.apply(transaction))
                                .whenComplete(($, $$) -> transaction.finalizeTransaction())
                );
    }

    @Override
    public StatementBuilder prepareStatement(String parameterizedSQL) {
        return new AmazonDataAPIStatementBuilder(this, parameterizedSQL, null);
    }
}
