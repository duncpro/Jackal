package com.duncpro.jackal.aws;

import com.duncpro.jackal.QueryResultRow;
import com.duncpro.jackal.SQLException;
import com.duncpro.jackal.SQLExecutor;
import com.duncpro.jackal.InterpolatedSQLStatement;
import software.amazon.awssdk.core.exception.SdkException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.duncpro.jackal.aws.AuroraServerlessConversions.compileAWSRequest;
import static com.duncpro.jackal.aws.AuroraServerlessConversions.extractRowsFromAWSResponse;

@ThreadSafe
public class AuroraServerlessSQLExecutor extends SQLExecutor {
    @Nullable
    final String transactionId;

    final AuroraServerlessClientBundle clients;
    final AuroraServerlessCredentials credentials;

    AuroraServerlessSQLExecutor(@Nullable final String transactionId, final AuroraServerlessClientBundle clients,
                                final AuroraServerlessCredentials credentials) {
        this.transactionId = transactionId;
        this.clients = clients;
        this.credentials = credentials;
    }

    @Override
    public CompletableFuture<Void> executeUpdateAsync(InterpolatedSQLStatement statement) {
        final var awsRequest = compileAWSRequest(this.credentials, statement, this.transactionId);
        final var awsFuture = this.clients.rdsDataAsyncClient.executeStatement(awsRequest);
        final var jackalFuture = new CompletableFuture<Void>();
        awsFuture.whenComplete((value, error) -> {
            boolean wasSuccessful = error == null;
            if (wasSuccessful) {
                jackalFuture.complete(null);
            } else {
                jackalFuture.completeExceptionally(new SQLException(error.getCause()));
            }
        });
        return jackalFuture;
    }

    @Override
    public void executeUpdate(InterpolatedSQLStatement statement) throws SQLException {
        final var request = compileAWSRequest(this.credentials, statement, this.transactionId);
        try {
            this.clients.rdsDataClient.executeStatement(request);
        } catch (SdkException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public CompletableFuture<Stream<QueryResultRow>> executeQueryAsync(InterpolatedSQLStatement statement) {
        final var awsRequest = compileAWSRequest(this.credentials, statement, this.transactionId);
        final var awsFuture = this.clients.rdsDataAsyncClient.executeStatement(awsRequest);
        final var jackalFuture = new CompletableFuture<Stream<QueryResultRow>>();
        awsFuture.whenComplete((response, error) -> {
            boolean wasSuccessful = error == null;
            if (wasSuccessful) {
                jackalFuture.complete(extractRowsFromAWSResponse(response));
            } else {
                jackalFuture.completeExceptionally(new SQLException(error.getCause()));
            }
        });
        return jackalFuture;
    }

    @Override
    public Stream<QueryResultRow> executeQuery(InterpolatedSQLStatement statement) throws SQLException {
        final var awsRequest = compileAWSRequest(this.credentials, statement, this.transactionId);
        try {
            final var response = this.clients.rdsDataClient.executeStatement(awsRequest);
            return extractRowsFromAWSResponse(response);
        } catch (SdkException e) {
            throw new SQLException(e);
        }
    }

    @Override
    protected Stream<QueryResultRow> executeQueryIncrementally(InterpolatedSQLStatement sql) {
        throw new UnsupportedOperationException("The RDS Aurora Serverless API does not support incremental result" +
                " fetching. For consistently small result sets #executeQuery will suffice. For larger datasets" +
                " consider using a database client implementation which supports streaming results, for example JDBC.");
    }
}
