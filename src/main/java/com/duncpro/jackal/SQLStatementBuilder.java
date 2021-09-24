package com.duncpro.jackal;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.CompletableFuture;
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
     * Executes a query on the database and returns the result as a stream of {@link QueryResultRow}.
     * To prevent resource leaks the caller should always close the stream after use.
     * @throws IllegalStateException if one or more parameters are missing arguments.
     */
    Stream<QueryResultRow> executeQuery();

    /**
     * Executes an update on the database and returns a {@link CompletableFuture} which completes after the
     * update has finished.
     */
    CompletableFuture<Void> executeUpdate();
}
