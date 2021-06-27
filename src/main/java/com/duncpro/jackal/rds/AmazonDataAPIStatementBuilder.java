package com.duncpro.jackal.rds;

import com.duncpro.jackal.QueryResultRow;
import com.duncpro.jackal.StatementBuilderBase;
import com.duncpro.jackal.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementResponse;
import software.amazon.awssdk.services.rdsdata.model.Field;
import software.amazon.awssdk.services.rdsdata.model.SqlParameter;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class AmazonDataAPIStatementBuilder extends StatementBuilderBase {
    private final AmazonDataAPIDatabase db;
    private final AmazonDataAPITransaction transaction;

    AmazonDataAPIStatementBuilder(AmazonDataAPIDatabase db, String sql, @Nullable AmazonDataAPITransaction transaction) {
        super(sql);
        this.db = db;
        this.transaction = transaction;
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

        if (transaction != null) {
            requestBuilder.transactionId(transaction.id);
        }

        return requestBuilder.build();
    }

    @Override
    protected Stream<QueryResultRow> executeQueryImpl() {
        final var resultStreamFuture = db.rdsDataClient.executeStatement(compileAWSRequest())
                .thenApply(AmazonDataAPIStatementBuilder::extractRowsFromAWSResponse)
                .thenApply(Collection::stream);

        return StreamUtil.unwrapStream(resultStreamFuture)
                .map(AmazonDataAPIRow::new);
    }

    @Override
    protected CompletableFuture<Void> executeUpdateImpl() {
        return db.rdsDataClient
                .executeStatement(compileAWSRequest())
                .thenApply(($) -> null);
    }

    protected SqlParameter[] compileArgs() {
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
            } else {
                throw new AssertionError("Unexpected type: " + arg.getClass().getName());
            }

            paramBuilder.value(fieldBuilder.build());

            awsStatementParams[i] = paramBuilder.build();
        }

        return awsStatementParams;
    }

    private static List<Map<String, Field>> extractRowsFromAWSResponse(ExecuteStatementResponse awsResponse) {
        final var rowList = new ArrayList<Map<String, Field>>();
        for (final var awsRowRecord : awsResponse.records()) {
            final var row = new HashMap<String, Field>();

            for (int columnIndex = 0; columnIndex < awsResponse.columnMetadata().size(); columnIndex++) {
                final var columnName = awsResponse.columnMetadata().get(columnIndex).name();
                final var typeName = awsResponse.columnMetadata().get(columnIndex).typeName();
                final var serializedValue = awsRowRecord.get(columnIndex);

                row.put(columnName, serializedValue);
            }

            rowList.add(row);
        }

        return rowList;
    }

    private static final Logger logger = LoggerFactory.getLogger(AmazonDataAPIStatementBuilder.class);
}
