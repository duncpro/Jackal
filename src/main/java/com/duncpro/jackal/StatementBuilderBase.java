package com.duncpro.jackal;

import org.intellij.lang.annotations.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public abstract class StatementBuilderBase implements StatementBuilder {
    @Language("SQL")
    protected final String parameterizedSQL;
    protected final int paramCount;
    protected final List<Object> args;

    public StatementBuilderBase(String parameterizedSQL) {
        this.parameterizedSQL = parameterizedSQL;
        paramCount = parameterizedSQL.length() - (parameterizedSQL
                .replaceAll(Pattern.quote("?"), "").length());
        args = new ArrayList<>(paramCount);
    }

    @Override
    public StatementBuilder withArguments(Object... args) {
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

    public final Stream<QueryResultRow> executeQuery() {
        if (args.size() < paramCount) {
            throw new IllegalStateException("Statement is incomplete. One or more parameters are missing arguments.");
        }
        return executeQueryImpl();
    }

    @Override
    public final CompletableFuture<Void> executeUpdate() {
        if (args.size() < paramCount) {
            throw new IllegalStateException("Statement is incomplete. One or more parameters are missing arguments.");
        }
        return executeUpdateImpl();
    }

    protected abstract Stream<QueryResultRow> executeQueryImpl();

    protected abstract CompletableFuture<Void> executeUpdateImpl();
}
