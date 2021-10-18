package com.duncpro.jackal.rds;

import com.duncpro.jackal.RelationalDatabaseException;
import com.duncpro.jackal.SQLStatementBuilder;
import com.duncpro.jackal.TransactionHandle;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.model.CommitTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.RollbackTransactionRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class AmazonDataAPITransactionHandle implements TransactionHandle {
    private boolean isCommitted = false;
    protected final String id;
    private final AmazonDataAPIDatabase db;

    AmazonDataAPITransactionHandle(String id, AmazonDataAPIDatabase db) {
        this.id = id;
        this.db = db;
    }

    @Override
    public SQLStatementBuilder prepareStatement(String parameterizedSQL) {
        return new AmazonDataAPIStatementBuilder(db, parameterizedSQL, this);
    }

    private CompletableFuture<Void> startRollback() {
        final var requestBuilder = RollbackTransactionRequest.builder()
                .secretArn(db.databaseSecretArn)
                .resourceArn(db.databaseArn);

        if (id != null) {
            requestBuilder.transactionId(id);
        }

        return db.rdsDataClient.rollbackTransaction(requestBuilder.build())
                .thenApply(($) -> null);
    }

    private void rollback() throws RelationalDatabaseException {
        try {
            startRollback().join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof SdkException) {
                throw new RelationalDatabaseException(e);
            }
            throw e;
        }
    }

    private CompletableFuture<Void> startCommit() {
        final var requestBuilder = CommitTransactionRequest.builder()
                .secretArn(db.databaseSecretArn)
                .resourceArn(db.databaseArn);

        if (id != null) {
            requestBuilder.transactionId(id);
        }

        return db.rdsDataClient.commitTransaction(requestBuilder.build())
                .thenApply(($) -> null);
    }

    @Override
    public void commit() throws RelationalDatabaseException {
        try {
            startCommit().join();
            isCommitted = true;
        } catch (CompletionException e) {
            if (e.getCause() instanceof SdkException) {
                throw new RelationalDatabaseException(e);
            }
            throw e;
        }
    }

    @Override
    public void close() throws RelationalDatabaseException {
        if (!isCommitted) {
            rollback();
        }
    }
}
