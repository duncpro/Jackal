package com.duncpro.jackal.aws;

import com.duncpro.jackal.SQLException;
import com.duncpro.jackal.SQLExecutor;
import com.duncpro.jackal.SQLTransaction;
import com.duncpro.jackal.util.Throwables;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.model.CommitTransactionRequest;
import software.amazon.awssdk.services.rdsdata.model.RollbackTransactionRequest;

import java.util.Objects;
import java.util.concurrent.CompletionException;

public class AuroraServerlessTransaction extends SQLTransaction {
    private volatile boolean isCommitted = false;
    private final AuroraServerlessSQLExecutor executor;

    public AuroraServerlessTransaction(final AuroraServerlessSQLExecutor executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public void commit() throws SQLException {
        final var request = CommitTransactionRequest.builder()
                .resourceArn(executor.credentials.dbArn)
                .secretArn(executor.credentials.dbSecretArn)
                .transactionId(executor.transactionId)
                .build();

        try {
            this.executor.rdsDataAsyncClient.commitTransaction(request).join();
        } catch (CompletionException e) {
            Throwables.unwrapAndThrow(e, SdkException.class, SQLException::new);
            throw new AssertionError();
        }

        isCommitted = true;
    }

    private void rollback() throws SQLException {
        final var request = RollbackTransactionRequest.builder()
                .resourceArn(this.executor.credentials.dbArn)
                .secretArn(this.executor.credentials.dbSecretArn)
                .transactionId(this.executor.transactionId)
                .build();

        try {
            this.executor.rdsDataAsyncClient.rollbackTransaction(request).join();
        } catch (CompletionException e) {
            Throwables.unwrapAndThrow(e, SdkException.class, SQLException::new);
            throw new AssertionError();
        }
    }

    @Override
    public void close() throws SQLException {
        if (!isCommitted) {
            rollback();
        }
    }

    @Override
    protected SQLExecutor getExecutor() {
        return executor;
    }
}
