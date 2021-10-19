package com.duncpro.jackal.aws;

import com.duncpro.jackal.RelationalDatabaseTransactionHandle;
import com.duncpro.jackal.SQLStatementBuilder;
import com.duncpro.jackal.RelationalDatabaseException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.model.CommitTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.RollbackTransactionRequest;

public class AuroraServerlessTransactionHandle implements RelationalDatabaseTransactionHandle {
    private final AuroraServerlessRelationalDatabase db;
    private final String transactionId;
    private boolean isCommitted = false;

    public AuroraServerlessTransactionHandle(AuroraServerlessRelationalDatabase db, String transactionId) {
        this.db = db;
        this.transactionId = transactionId;
    }

    @Override
    public void commit() throws RelationalDatabaseException {
        final var request = CommitTransactionRequest.builder()
                .resourceArn(this.db.dbArn)
                .secretArn(this.db.dbSecretArn)
                .transactionId(this.transactionId)
                .build();

        try {
            this.db.rdsDataClient.commitTransaction(request);
        } catch (SdkException e) {
            throw new RelationalDatabaseException(e);
        }

        isCommitted = true;
    }

    private void rollback() throws RelationalDatabaseException {
        final var request = RollbackTransactionRequest.builder()
                .resourceArn(this.db.dbArn)
                .secretArn(this.db.dbSecretArn)
                .transactionId(this.transactionId)
                .build();

        try {
            db.rdsDataClient.rollbackTransaction(request);
        } catch (SdkException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public SQLStatementBuilder prepareStatement(String parameterizedSQL) {
        return new AuroraServerlessStatementBuilder(this.db, parameterizedSQL, transactionId);
    }

    @Override
    public void close() throws RelationalDatabaseException {
        if (!isCommitted) {
            rollback();
        }
    }
}
