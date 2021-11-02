package com.duncpro.jackal;

import org.intellij.lang.annotations.Language;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.List.copyOf;

@Immutable
public final class InterpolatableSQLStatement {
    final ParameterizableSQLStatement sql;
    final List<Object> args;

    InterpolatableSQLStatement(final ParameterizableSQLStatement sql, final List<Object> args) {
        this.sql = sql;
        this.args = copyOf(args);
    }

    /**
     * Creates a new SQL statement which is equivalent to this one, except for that the given arguments
     * have been appended to the end of the arguments list.
     * @throws IndexOutOfBoundsException if after appending the given arguments, the argument count would exceed the
     * number of parameters in the SQL script.
     */
    public InterpolatableSQLStatement withArguments(final Object... args) {
        final var totalArgs = new ArrayList<>();
        totalArgs.addAll(this.args);
        totalArgs.addAll(asList(args));
        if (totalArgs.size() > sql.countParameters()) throw new IndexOutOfBoundsException();
        return new InterpolatableSQLStatement(sql, totalArgs);
    }

    int countArguments() {
        return this.args.size();
    }

    /**
     * Verifies that this {@link InterpolatableSQLStatement} contains exactly the number of arguments
     * that there are parameters in the sql script. If this condition is true, an {@link InterpolatedSQLStatement}
     * is returned, otherwise an {@link IllegalStateException} is thrown.
     */
    private InterpolatedSQLStatement verifyFullyInterpolated() {
        return new InterpolatedSQLStatement(this);
    }

    // Extension Functions
    public CompletableFuture<Void> executeUpdateAsync(SQLExecutorProvider database) {
        return database.getExecutor().executeUpdateAsync(this.verifyFullyInterpolated());
    }
    public void executeUpdate(SQLExecutorProvider database) throws SQLException {
        database.getExecutor().executeUpdate(this.verifyFullyInterpolated());
    }

    public CompletableFuture<Stream<QueryResultRow>> executeQueryAsync(SQLExecutorProvider database) {
        return database.getExecutor().executeQueryAsync(this.verifyFullyInterpolated());
    }
    public Stream<QueryResultRow> executeQuery(SQLExecutorProvider database) throws SQLException {
        return database.getExecutor().executeQuery(this.verifyFullyInterpolated());
    }

    // Factory Functions

    public static InterpolatableSQLStatement sql(@Language("SQL") final String script) {
        return new InterpolatableSQLStatement(new ParameterizableSQLStatement(script), emptyList());
    }


}
