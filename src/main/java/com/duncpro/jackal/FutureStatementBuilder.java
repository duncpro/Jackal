package com.duncpro.jackal;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class FutureStatementBuilder extends StatementBuilderBase {
    private final CompletableFuture<SQLStatementExecutor> statementExecutorFuture;
    public FutureStatementBuilder(CompletableFuture<SQLStatementExecutor> statementExecutorFuture, String parameterizedSQL) {
        super(parameterizedSQL);
        this.statementExecutorFuture = statementExecutorFuture;
    }

    @Override
    protected Stream<QueryResultRow> executeQueryImpl() {
        final var futureStream = statementExecutorFuture.thenApply(executor ->
            executor.prepareStatement(this.parameterizedSQL)
                    .withArguments(this.args)
                    .query()
        );
        return StreamUtil.unwrapStream(futureStream);
    }

    @Override
    protected CompletableFuture<Void> executeUpdateImpl() {
        return statementExecutorFuture.thenCompose(executor ->
                executor.prepareStatement(this.parameterizedSQL)
                        .withArguments(this.args)
                        .executeUpdate()
        );
    }
}
