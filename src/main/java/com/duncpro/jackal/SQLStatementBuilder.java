package com.duncpro.jackal;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NotThreadSafe
public interface SQLStatementBuilder {
    /**
     * Effectively interpolates the given arguments into the SQL statement. The first argument in the list
     * is interpolated into the first parameter and so on. The following argument types are supported {@link String},
     * {@link Integer}, {@code byte[]}, {@link Double}, {@link Long}, {@link Boolean}.
     * @param args an array of arguments to append to the end of the argument list.
     * @throws IndexOutOfBoundsException if the number of arguments passed exceeds the number of parameters
     * within the SQL statement.
     * @throws IllegalArgumentException if an unsupported type is passed as an argument.
     */
    SQLStatementBuilder withArguments(Object... args);

    /**
     * Appends the given argument to the end of the argument list.
     * This is an alias for {@link #withArguments}. See the aforementioned function's declaration
     * for complete documentation.
     */
    default SQLStatementBuilder withArgument(Object arg) {
        return withArguments(arg);
    }

    /**
     * Executes the query and immediately fetches the entire result set from the database.
     * This function has a number of advantages over {@link #startQuery()} but should only
     * be used with queries that contain a LIMIT clause. This function automatically closes
     * all resources associated with the query, regardless of if an exception is thrown.
     * There is no need to wrap the returned stream in a try-with-resources block.
     * @throws RelationalDatabaseException if an error occurs while fetching ANY of the results
     * from the database.
     */
    default Stream<QueryResultRow> executeQuery() throws RelationalDatabaseException {
        final List<QueryResultRow> bufferedResults;
        try (final var results = startQuery()) {
            bufferedResults = results.collect(Collectors.toList());
        } catch (UncheckedRelationalDatabaseException e) {
            throw e.getCause();
        }
        return bufferedResults.stream();
    }

    /**
     * Executes a query on the database and returns the result as a stream of {@link QueryResultRow}.
     *
     * Some implementations might lazily fetch rows from the database. To prevent resource leaks the caller
     * should always close the stream after use. The method {@link #executeQuery()} offers an alternative
     * to this method which prefetches all results from the database and closes the stream automatically.
     * For (LIMIT)ed result sets consider using the more ergonomic {@link #executeQuery()} function.
     *
     * If an error occurs while executing the query, an {@link UncheckedRelationalDatabaseException}
     * will be thrown upon invoking a terminal operator on the returned stream. This exception should
     * be treated with the semantics of a checked exception.
     *
     * @throws IllegalStateException if one or more parameters are missing arguments.
     */
    Stream<QueryResultRow> startQuery();

    /**
     * Executes an update on the database and returns a {@link CompletableFuture} which completes after the
     * update has finished.
     */
    CompletableFuture<Void> startUpdate();

    /**
     * Executes an update on the database and blocks the current thread until the update has completed.s
     */
    default void executeUpdate() throws RelationalDatabaseException {
        try {
            startUpdate().join();
        } catch (CompletionException e) {
            FutureUtils.unwrapCompletionException(e);
        }
    }
}
