package com.duncpro.jackal.aws;

import com.duncpro.jackal.*;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static com.duncpro.jackal.aws.AuroraServerlessConversions.compileAWSRequest;
import static com.duncpro.jackal.aws.AuroraServerlessConversions.extractRowsFromAWSResponse;

@ThreadSafe
public class AuroraServerlessSQLExecutor extends SQLExecutor {
    @Nullable
    final String transactionId;

    final RdsDataAsyncClient rdsDataAsyncClient;
    final AuroraServerlessCredentials credentials;

    AuroraServerlessSQLExecutor(@Nullable final String transactionId, final RdsDataAsyncClient rdsDataAsyncClient,
                                final AuroraServerlessCredentials credentials) {
        this.transactionId = transactionId;
        this.rdsDataAsyncClient = Objects.requireNonNull(rdsDataAsyncClient);
        this.credentials = Objects.requireNonNull(credentials);
    }

    @Override
    public CompletableFuture<Void> executeUpdateAsync(InterpolatedSQLStatement statement) {
        final var awsRequest = compileAWSRequest(this.credentials, statement, this.transactionId);
        final var awsFuture = this.rdsDataAsyncClient.executeStatement(awsRequest);
        final var jackalFuture = new CompletableFuture<Void>();
        awsFuture.whenComplete((value, error) -> {
            boolean wasSuccessful = error == null;
            if (wasSuccessful) {
                jackalFuture.complete(null);
            } else {
                final var cause = Throwables.unwrapCompletionException(error);
                jackalFuture.completeExceptionally(cause instanceof SdkException ? new SQLException(cause) : cause);
            }
        });
        return jackalFuture;
    }

    @Override
    public void executeUpdate(InterpolatedSQLStatement statement) throws SQLException {
        try {
            this.executeUpdateAsync(statement).join();
        } catch (CompletionException e) {
            Throwables.unwrapAndThrow(e, SQLException.class);
            throw new AssertionError();
        }
    }

    @Override
    public CompletableFuture<Stream<QueryResultRow>> executeQueryAsync(InterpolatedSQLStatement statement) {
        final var awsRequest = compileAWSRequest(this.credentials, statement, this.transactionId);
        final var awsFuture = this.rdsDataAsyncClient.executeStatement(awsRequest);
        final var jackalFuture = new CompletableFuture<Stream<QueryResultRow>>();
        awsFuture.whenComplete((response, error) -> {
            boolean wasSuccessful = error == null;
            if (wasSuccessful) {
                jackalFuture.complete(extractRowsFromAWSResponse(response));
            } else {
                final var cause = Throwables.unwrapCompletionException(error);
                jackalFuture.completeExceptionally(cause instanceof SdkException ? new SQLException(cause) : cause);
            }
        });
        return jackalFuture;
    }

    @Override
    public Stream<QueryResultRow> executeQuery(InterpolatedSQLStatement statement) throws SQLException {
        try {
            return executeQueryAsync(statement).join();
        } catch (CompletionException e) {
            Throwables.unwrapAndThrow(e, SQLException.class);
            throw new AssertionError();
        }
    }

    @Override
    protected Stream<QueryResultRow> executeQueryIncrementally(InterpolatedSQLStatement sql) {
        throw new UnsupportedOperationException("The RDS Aurora Serverless API does not support incremental result" +
                " fetching. For consistently small result sets #executeQuery will suffice. For larger datasets" +
                " consider using a database client implementation which supports streaming results, for example JDBC.");
    }
}
