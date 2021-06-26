package com.duncpro.jackal.impl;

import com.duncpro.jackal.QueryResultRow;
import com.duncpro.jackal.StatementBuilderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementResponse;
import software.amazon.awssdk.services.rdsdata.model.Field;
import software.amazon.awssdk.services.rdsdata.model.SqlParameter;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class AmazonRDSStatementBuilder extends StatementBuilderBase {
    private final AmazonDataAPIDatabase db;
    private final String transactionId;

    AmazonRDSStatementBuilder(AmazonDataAPIDatabase db, String sql, @Nullable String transactionId) {
        super(sql);
        this.db = db;
        this.transactionId = transactionId;
    }

    protected String compileSQL() {
        String compiledSql = parameterizedSQL;

        for (int i = 0; i < paramCount; i++) {
            compiledSql =
                    compiledSql.replaceFirst(Pattern.quote("?"), ":" + i);
        }

        return compiledSql;
    }

    private ExecuteStatementRequest compileAWSRequest() {
        final var requestBuilder = ExecuteStatementRequest.builder()
                .resourceArn(db.databaseArn)
                .secretArn(db.databaseSecretArn)
                .sql(compileSQL())
                .includeResultMetadata(true)
                .parameters(compileArgs());

        if (transactionId != null) {
            requestBuilder.transactionId(transactionId);
        }

        return requestBuilder.build();
    }

    @Override
    protected Stream<QueryResultRow> executeQueryImpl() {
        final var resultStreamFuture = db.rdsDataClient.executeStatement(compileAWSRequest())
                .thenApply(AmazonRDSStatementBuilder::extractRowsFromAWSResponse)
                .thenApply(Collection::stream);

        final Supplier<Spliterator<QueryResultRow>> supplier = () ->
                resultStreamFuture
                        .join()
                        .map(QueryResultRow::fromMap)
                        .spliterator();

        return StreamSupport
                .stream(supplier, Spliterator.ORDERED|Spliterator.SIZED|Spliterator.SUBSIZED|Spliterator.IMMUTABLE,
                        false);
    }

    @Override
    protected CompletableFuture<Void> executeUpdateImpl() {
        return db.rdsDataClient
                .executeStatement(compileAWSRequest())
                .thenApply(($) -> null);
    }

    protected SqlParameter[] compileArgs() {
        SqlParameter[] awsStatementParams = new SqlParameter[countArgs()];

        for (int i = 0; i < args.length; i++) {
            final var arg = args[i];
            final var paramBuilder = SqlParameter.builder().name(String.valueOf(i));

            final Field field;

            if (arg instanceof String) {
                field = Field.builder()
                        .stringValue((String) arg)
                        .build();
            } else if (arg instanceof Long) {
                field = Field.builder()
                        .longValue((Long) arg)
                        .build();
            } else if (arg instanceof Boolean) {
                field = Field.builder()
                        .booleanValue((Boolean) arg)
                        .build();
            } else {
                throw new AssertionError("Unexpected type: " + arg.getClass().getName());
            }

            paramBuilder.value(field);

            awsStatementParams[i] = paramBuilder.build();
        }

        return awsStatementParams;
    }

    private static List<Map<String, Object>> extractRowsFromAWSResponse(ExecuteStatementResponse awsResponse) {
        final var rowList = new ArrayList<Map<String, Object>>();
        for (final var awsRowRecord : awsResponse.records()) {
            final var row = new HashMap<String, Object>();

            for (int columnIndex = 0; columnIndex < awsResponse.columnMetadata().size(); columnIndex++) {
                final var columnName = awsResponse.columnMetadata().get(columnIndex).name();
                final var typeName = awsResponse.columnMetadata().get(columnIndex).typeName();
                final var serializedValue = awsRowRecord.get(columnIndex);

                Object deserializedValue;

                if (typeName.equals("varchar")) {
                    deserializedValue = serializedValue.stringValue();
                } else if (typeName.equals("bool")) {
                    deserializedValue = serializedValue.booleanValue();
                } else if (typeName.contains("int")) {
                    deserializedValue = serializedValue.longValue();
                } else {
                    throw new AssertionError("Unexpected data type: " + typeName);
                }

                row.put(columnName, deserializedValue);
            }

            rowList.add(row);
        }

        return rowList;
    }

    private static final Logger logger = LoggerFactory.getLogger(AmazonRDSStatementBuilder.class);
}
