package com.duncpro.jackal.impl;

import com.duncpro.jackal.AsyncDatabaseTransaction;
import com.duncpro.jackal.StatementBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rdsdata.model.CommitTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.RollbackTransactionRequest;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
class AmazonRDSTransaction implements AsyncDatabaseTransaction {
    private final AmazonDataAPIDatabase db;
    private final String transactionId;

    @Override
    public CompletableFuture<Void> rollback() {
        final var requestBuilder = RollbackTransactionRequest.builder()
                .secretArn(db.databaseSecretArn)
                .resourceArn(db.databaseArn);

        if (transactionId != null) {
            requestBuilder.transactionId(transactionId);
        }

        return db.rdsDataClient.rollbackTransaction(requestBuilder.build())
                .whenComplete((response, $) -> {
                    if (response != null) {
                        logger.debug("Database Transaction (id: " + transactionId + ") rollback finalized with" +
                                " transaction status " + response.transactionStatus() + ".");
                    }
                })
                .thenApply(($) -> null);
    }

    @Override
    public CompletableFuture<Void> commit() {
        final var requestBuilder = CommitTransactionRequest.builder()
                .secretArn(db.databaseSecretArn)
                .resourceArn(db.databaseArn);

        if (transactionId != null) {
            requestBuilder.transactionId(transactionId);
        }

        return db.rdsDataClient.commitTransaction(requestBuilder.build())
                .whenComplete((response, $) -> {
                    if (response != null) {
                        logger.debug("Database Transaction (id: " + transactionId + ") commit finalized with" +
                                " transaction status " + response.transactionStatus() + ".");
                    }
                })
                .thenApply(($) -> null);
    }

    @Override
    public StatementBuilder prepareStatement(String parameterizedSQL) {
        return new AmazonRDSStatementBuilder(db, parameterizedSQL, transactionId);
    }

    private final static Logger logger = LoggerFactory.getLogger(AmazonRDSTransaction.class);
}
