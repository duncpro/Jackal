package com.duncpro.jackal.aws;

import com.duncpro.jackal.RelationalDatabaseException;
import com.duncpro.jackal.SQLStatementBuilderBase;
import com.duncpro.jackal.QueryResultRow;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementResponse;
import software.amazon.awssdk.services.rdsdata.model.Field;
import software.amazon.awssdk.services.rdsdata.model.SqlParameter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class AuroraServerlessStatementBuilder extends SQLStatementBuilderBase {
    private final AuroraServerlessRelationalDatabase db;
    private final String transactionId;

    public AuroraServerlessStatementBuilder(AuroraServerlessRelationalDatabase db, String parameterizedSQL,
                                            String transactionId) {
        super(parameterizedSQL);
        this.db = db;
        this.transactionId = transactionId;
    }

    private String compileSQL() {
        String compiledSql = parameterizedSQL;
        for (int i = 0; i < paramCount; i++) {
            compiledSql = compiledSql.replaceFirst(Pattern.quote("?"), ":" + i);
        }
        return compiledSql;
    }

    private ExecuteStatementRequest compileAWSRequest() {
        final var requestBuilder = ExecuteStatementRequest.builder()
                .resourceArn(db.dbArn)
                .secretArn(db.dbSecretArn)
                .sql(compileSQL())
                .includeResultMetadata(true)
                .parameters(compileArgs());

        if (transactionId != null)
            requestBuilder.transactionId(transactionId);

        return requestBuilder.build();
    }

    private SqlParameter[] compileArgs() {
        SqlParameter[] awsStatementParams = new SqlParameter[args.size()];
        for (int i = 0; i < args.size(); i++) {
            final var arg = args.get(i);
            final var paramBuilder = SqlParameter.builder().name(String.valueOf(i));
            final Field.Builder fieldBuilder = Field.builder();
            if (arg == null) {
                fieldBuilder.isNull(true);
            } else if (arg instanceof String) {
                fieldBuilder.stringValue((String) arg);
            } else if (arg instanceof Integer) {
                fieldBuilder.longValue(Long.valueOf((Integer) arg));
            } else if (arg instanceof Long) {
                fieldBuilder.longValue((Long) arg);
            } else if (arg instanceof Boolean) {
                fieldBuilder.booleanValue((Boolean) arg);
            } else if (arg instanceof Double) {
                fieldBuilder.doubleValue((Double) arg);
            } else if (arg instanceof byte[]) {
                fieldBuilder.blobValue(SdkBytes.fromByteArray((byte[]) arg));
            } else if (arg instanceof UUID) {
                fieldBuilder.stringValue(arg.toString());
                paramBuilder.typeHint("UUID");
            }  else {
                throw new AssertionError("Unexpected type: " + arg.getClass().getName());
            }
            paramBuilder.value(fieldBuilder.build());
            awsStatementParams[i] = paramBuilder.build();
        }

        return awsStatementParams;
    }

    private static Stream<QueryResultRow> extractRowsFromAWSResponse(ExecuteStatementResponse awsResponse) {
        final var rawRows = new ArrayList<Map<String, Field>>();
        for (final var awsRowRecord : awsResponse.records()) {
            final var row = new HashMap<String, Field>();

            for (int columnIndex = 0; columnIndex < awsResponse.columnMetadata().size(); columnIndex++) {
                final var columnName = awsResponse.columnMetadata().get(columnIndex).name();
                final var typeName = awsResponse.columnMetadata().get(columnIndex).typeName();
                final var serializedValue = awsRowRecord.get(columnIndex);
                row.put(columnName, serializedValue);
            }

            rawRows.add(row);
        }
        return rawRows.stream().map(AuroraServerlessRow::new);
    }

    @Override
    public CompletableFuture<Void> executeUpdateAsync() {
        super.executeUpdateAsync();
        final var awsFuture = this.db.rdsDataAsyncClient.executeStatement(compileAWSRequest());
        final var jackalFuture = new CompletableFuture<Void>();
        awsFuture.whenComplete((value, error) -> {
            boolean wasSuccessful = error == null;
            if (wasSuccessful) {
                jackalFuture.complete(null);
            } else {
                jackalFuture.completeExceptionally(new RelationalDatabaseException(error.getCause()));
            }
        });
        return jackalFuture;
    }

    @Override
    public void executeUpdate() throws RelationalDatabaseException {
        super.executeUpdate();
        try {
            this.db.rdsDataClient.executeStatement(compileAWSRequest());
        } catch (SdkException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public CompletableFuture<Stream<QueryResultRow>> executeQueryAsync() {
        super.executeQueryAsync();
        final var awsFuture = db.rdsDataAsyncClient.executeStatement(compileAWSRequest());
        final var jackalFuture = new CompletableFuture<Stream<QueryResultRow>>();
        awsFuture.whenComplete((response, error) -> {
            boolean wasSuccessful = error == null;
            if (wasSuccessful) {
                jackalFuture.complete(extractRowsFromAWSResponse(response));
            } else {
                jackalFuture.completeExceptionally(new RelationalDatabaseException(error.getCause()));
            }
        });
        return jackalFuture;
    }

    @Override
    public Stream<QueryResultRow> executeQuery() throws RelationalDatabaseException {
        super.executeQuery();
        try {
            final var response = this.db.rdsDataClient.executeStatement(compileAWSRequest());
            return extractRowsFromAWSResponse(response);
        } catch (SdkException e) {
            throw new RelationalDatabaseException(e);
        }
    }
}
