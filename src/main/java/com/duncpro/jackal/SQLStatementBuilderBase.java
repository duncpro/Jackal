package com.duncpro.jackal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.stream.Collectors.toList;

public class SQLStatementBuilderBase implements SQLStatementBuilder {
    protected final String parameterizedSQL;
    protected final int paramCount;
    protected final List<Object> args;

    public SQLStatementBuilderBase(String parameterizedSQL) {
        this.parameterizedSQL = parameterizedSQL;
        paramCount = parameterizedSQL.length() - (parameterizedSQL
                .replaceAll(Pattern.quote("?"), "").length());
        args = new ArrayList<>(paramCount);
    }

    @Override
    public final SQLStatementBuilder withArguments(Object... args) {
        if (args.length + this.args.size() > paramCount) {
            throw new IndexOutOfBoundsException("There are " + paramCount + " parameters in the statement" +
                    " but " + (args.length + this.args.size()) + " arguments have been given. (Did you forget to " +
                    "include a parameter in the statement?)");
        }
        this.args.addAll(
                Arrays.stream(args).collect(toList())
        );
        return this;
    }

    private void assertEnoughArgs() {
        if (args.size() < paramCount) {
            throw new IllegalStateException("Statement is incomplete. One or more parameters are missing arguments.");
        }
    }

    @Override
    public CompletableFuture<Void> executeUpdateAsync() {
        assertEnoughArgs();
        return failedFuture(new UnsupportedOperationException());
    }

    @Override
    public void executeUpdate() throws RelationalDatabaseException {
        assertEnoughArgs();
    }

    @Override
    public CompletableFuture<Stream<QueryResultRow>> executeQueryAsync() {
        assertEnoughArgs();
        return failedFuture(new UnsupportedOperationException());
    }

    @Override
    public Stream<QueryResultRow> executeQuery() throws RelationalDatabaseException {
        assertEnoughArgs();
        return Stream.generate(() -> { throw new UnsupportedOperationException(); });
    }
}
