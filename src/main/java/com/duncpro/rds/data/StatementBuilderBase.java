package com.duncpro.rds.data;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public abstract class StatementBuilderBase implements StatementBuilder {
    protected final String parameterizedSQL;
    protected final int paramCount;
    protected final Object[] args;

    public StatementBuilderBase(String parameterizedSQL) {
        this.parameterizedSQL = parameterizedSQL;
        paramCount = parameterizedSQL.length() - (parameterizedSQL
                .replaceAll(Pattern.quote("?"), "").length());
        args = new Object[paramCount];
    }

    protected int countArgs() {
        final var count = Arrays.stream(args)
                .filter(Objects::nonNull)
                .count();

        return Long.valueOf(count).intValue();
    }

    public StatementBuilder setString(int paramIndex, String arg) {
        args[paramIndex] = arg;
        return this;
    }

    @Override
    public StatementBuilder setLong(int paramIndex, Long arg) {
        args[paramIndex] = arg;
        return this;
    }

    @Override
    public StatementBuilder setBoolean(int paramIndex, Boolean arg) {
        args[paramIndex] = arg;
        return this;
    }

    public final CompletableFuture<QueryResult> executeQuery() {
        if (countArgs() < paramCount) {
            throw new IllegalStateException("Statement is incomplete. One or more parameters are missing arguments.");
        }

        return executeQueryImpl();
    }

    @Override
    public final CompletableFuture<Void> executeUpdate() {
        if (countArgs() < paramCount) {
            throw new IllegalStateException("Statement is incomplete. One or more parameters are missing arguments.");
        }
        return executeUpdateImpl();
    }

    protected abstract CompletableFuture<QueryResult> executeQueryImpl();

    protected abstract CompletableFuture<Void> executeUpdateImpl();
}
