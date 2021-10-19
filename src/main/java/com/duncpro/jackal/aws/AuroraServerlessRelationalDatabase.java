package com.duncpro.jackal.aws;

import com.duncpro.jackal.RelationalDatabase;
import com.duncpro.jackal.RelationalDatabaseTransactionHandle;
import com.duncpro.jackal.SQLStatementBuilder;
import com.duncpro.jackal.RelationalDatabaseException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionRequest;

public class AuroraServerlessRelationalDatabase implements RelationalDatabase {
    final RdsDataClient rdsDataClient;
    final RdsDataAsyncClient rdsDataAsyncClient;
    final String dbArn;
    final String dbSecretArn;

    public AuroraServerlessRelationalDatabase(RdsDataClient rdsDataClient, RdsDataAsyncClient rdsDataAsyncClient,
                                              String dbArn, String dbSecretArn) {
        this.rdsDataClient = rdsDataClient;
        this.rdsDataAsyncClient = rdsDataAsyncClient;
        this.dbArn = dbArn;
        this.dbSecretArn = dbSecretArn;
    }

    @Override
    public RelationalDatabaseTransactionHandle startTransaction() throws RelationalDatabaseException {
        final var request = BeginTransactionRequest.builder()
                .resourceArn(dbArn)
                .secretArn(dbSecretArn)
                .build();

        try {
            final var transactionId = rdsDataClient.beginTransaction(request).transactionId();
            return new AuroraServerlessTransactionHandle(this, transactionId);
        } catch (SdkException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public SQLStatementBuilder prepareStatement(String parameterizedSQL) {
        return new AuroraServerlessStatementBuilder(this, parameterizedSQL, null);
    }
}
