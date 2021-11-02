package com.duncpro.jackal.aws;

import com.duncpro.jackal.QueryResultRow;
import com.duncpro.jackal.InterpolatedSQLStatement;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementResponse;
import software.amazon.awssdk.services.rdsdata.model.Field;
import software.amazon.awssdk.services.rdsdata.model.SqlParameter;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class AuroraServerlessConversions {
    private AuroraServerlessConversions() {}

    static String compileSQL(InterpolatedSQLStatement statement) {
        String compiledSql = statement.getParameterizedScript();
        for (int i = 0; i < statement.getArgs().size(); i++) {
            compiledSql = compiledSql.replaceFirst(Pattern.quote("?"), ":" + i);
        }
        return compiledSql;
    }

    static ExecuteStatementRequest compileAWSRequest(final AuroraServerlessCredentials credentials,
                                                             final InterpolatedSQLStatement statement,
                                                             @Nullable final String transactionId) {
        final var requestBuilder = ExecuteStatementRequest.builder()
                .resourceArn(credentials.dbArn)
                .secretArn(credentials.dbSecretArn)
                .sql(compileSQL(statement))
                .includeResultMetadata(true)
                .parameters(compileArgs(statement.getArgs()));

        if (transactionId != null)
            requestBuilder.transactionId(transactionId);

        return requestBuilder.build();
    }

    static SqlParameter[] compileArgs(List<Object> args) {
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

    static Stream<QueryResultRow> extractRowsFromAWSResponse(ExecuteStatementResponse awsResponse) {
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
}
