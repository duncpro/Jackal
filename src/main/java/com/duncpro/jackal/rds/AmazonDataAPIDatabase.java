package com.duncpro.jackal.rds;

import com.duncpro.jackal.*;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionResponse;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Exposes {@link RdsDataClient} as an {@link RelationalDatabase}.
 */
@ThreadSafe
public class AmazonDataAPIDatabase implements RelationalDatabase {
    final RdsDataAsyncClient rdsDataClient;
    final String databaseArn;
    final String databaseSecretArn;

    /**
     * @param rdsDataClient the {@link RdsDataAsyncClient} client which will be used to communicate with AWS.
     * @param databaseArn the ARN of the AWS RDS Database Cluster to connect to.
     * @param databaseSecretArn the ARN of the AWS SecretsManager Secret which contains the credentials for this database.
     */
    public AmazonDataAPIDatabase(RdsDataAsyncClient rdsDataClient, String databaseArn,
                                 String databaseSecretArn) {
        this.rdsDataClient = rdsDataClient;
        this.databaseArn = databaseArn;
        this.databaseSecretArn = databaseSecretArn;
    }

    @Override
    public TransactionHandle startTransaction() throws RelationalDatabaseException {
        final var request = BeginTransactionRequest.builder()
                .secretArn(databaseSecretArn)
                .resourceArn(databaseArn)
                .build();

        final var transactionId = rdsDataClient.beginTransaction(request)
                .thenApply(BeginTransactionResponse::transactionId);

        try {
            return new AmazonDataAPITransactionHandle(transactionId.join(), this);
        } catch (CompletionException e) {
            if (e.getCause() instanceof SdkException) {
                throw new RelationalDatabaseException(e.getCause());
            }
            throw e;
        }
    }

    @Override
    public SQLStatementBuilder prepareStatement(String parameterizedSQL) {
        return new AmazonDataAPIStatementBuilder(this, parameterizedSQL, null);
    }
}
