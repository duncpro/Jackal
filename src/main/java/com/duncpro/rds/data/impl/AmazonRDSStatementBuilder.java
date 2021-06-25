package com.duncpro.rds.data.impl;

import com.duncpro.rds.data.QueryResult;
import com.duncpro.rds.data.StatementBuilderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.rdsdata.model.Field;
import software.amazon.awssdk.services.rdsdata.model.SqlParameter;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

class AmazonRDSStatementBuilder extends StatementBuilderBase {
    private final AmazonRDSAsyncDatabaseWrapper db;
    private final String transactionId;

    AmazonRDSStatementBuilder(AmazonRDSAsyncDatabaseWrapper db, String sql, @Nullable String transactionId) {
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

    @Override
    protected CompletableFuture<QueryResult> executeQueryImpl() {
        final var requestBuilder = ExecuteStatementRequest.builder()
                .resourceArn(db.databaseArn)
                .secretArn(db.databaseSecretArn)
                .sql(compileSQL())
                .includeResultMetadata(true)
                .parameters(compileArgs());

        if (transactionId != null) {
            requestBuilder.transactionId(transactionId);
        }

        return db.rdsDataClient.executeStatement(requestBuilder.build())
                .thenApply(AmazonRDSQueryResult::new);
    }

    @Override
    protected CompletableFuture<Void> executeUpdateImpl() {
        return executeQueryImpl().thenApply(($) -> null);
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

    private static final Logger logger = LoggerFactory.getLogger(AmazonRDSStatementBuilder.class);
}
