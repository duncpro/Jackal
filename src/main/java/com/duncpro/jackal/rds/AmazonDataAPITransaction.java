package com.duncpro.jackal.rds;

import com.duncpro.jackal.AsyncDatabaseTransaction;
import com.duncpro.jackal.StatementBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rdsdata.model.CommitTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.RollbackTransactionRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
class AmazonDataAPITransaction implements AsyncDatabaseTransaction {
    private final AmazonDataAPIDatabase db;
    final String id;

    volatile AtomicInteger pendingOperations = new AtomicInteger(0);

    @Override
    public CompletableFuture<Void> rollback() {
        final var requestBuilder = RollbackTransactionRequest.builder()
                .secretArn(db.databaseSecretArn)
                .resourceArn(db.databaseArn);

        if (id != null) {
            requestBuilder.transactionId(id);
        }

        pendingOperations.incrementAndGet();

        return db.rdsDataClient.rollbackTransaction(requestBuilder.build())
                .whenComplete((response, $) -> {
                    if (response != null) {
                        logger.debug("Database Transaction (id: " + id + ") rollback finalized with" +
                                " transaction status " + response.transactionStatus() + ".");
                    }
                })
                .whenComplete(($, $$) -> pendingOperations.decrementAndGet())
                .thenApply(($) -> null);
    }

    @Override
    public CompletableFuture<Void> commit() {
        final var requestBuilder = CommitTransactionRequest.builder()
                .secretArn(db.databaseSecretArn)
                .resourceArn(db.databaseArn);

        if (id != null) {
            requestBuilder.transactionId(id);
        }

        pendingOperations.incrementAndGet();

        return db.rdsDataClient.commitTransaction(requestBuilder.build())
                .whenComplete((response, $) -> {
                    if (response != null) {
                        logger.debug("Database Transaction (id: " + id + ") commit finalized with" +
                                " transaction status " + response.transactionStatus() + ".");
                    }
                })
                .whenComplete(($, $$) -> pendingOperations.decrementAndGet())
                .thenApply(($) -> null);
    }

    public void finalizeTransaction() {
        if (this.pendingOperations.get() != 0) {
            throw new IllegalStateException("Transaction procedure returned before all operations" +
                    " were completed. You should join all promises and terminate all streams before" +
                    " returning.");
        }
    }

    @Override
    public StatementBuilder prepareStatement(String parameterizedSQL) {
        return new AmazonDataAPIStatementBuilder(db, parameterizedSQL, this);
    }

    private final static Logger logger = LoggerFactory.getLogger(AmazonDataAPITransaction.class);
}
