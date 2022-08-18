package com.duncpro.jackal;

import org.intellij.lang.annotations.Language;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
        // Cannot be replaced, without disallowing null values
        //noinspection Java9CollectionFactory
        this.args = Collections.unmodifiableList(new ArrayList<>(args));
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
        Objects.requireNonNull(database);
        final var verified = this.verifyFullyInterpolated();
        return database.getExecutor().executeUpdateAsync(verified);
    }
    public void executeUpdate(SQLExecutorProvider database) throws SQLException {
        Objects.requireNonNull(database);
        final var verified = this.verifyFullyInterpolated();
        database.getExecutor().executeUpdate(verified);
    }

    public CompletableFuture<Stream<QueryResultRow>> executeQueryAsync(SQLExecutorProvider database) {
        Objects.requireNonNull(database);
        final var verified = this.verifyFullyInterpolated();
        return database.getExecutor().executeQueryAsync(verified);
    }

    public Stream<QueryResultRow> executeQuery(SQLExecutorProvider database) throws SQLException {
        Objects.requireNonNull(database);
        final var verified = this.verifyFullyInterpolated();
        return database.getExecutor().executeQuery(verified);
    }

    public Stream<QueryResultRow> executeQueryIncrementally(SQLExecutorProvider database) {
        Objects.requireNonNull(database);
        final var verified = this.verifyFullyInterpolated();
        return database.getExecutor().executeQueryIncrementally(verified);
    }

    // Factory Functions
    public static InterpolatableSQLStatement sql(@Language("SQL") final String script) {
        Objects.requireNonNull(script);
        return new InterpolatableSQLStatement(new ParameterizableSQLStatement(script), emptyList());
    }
}
