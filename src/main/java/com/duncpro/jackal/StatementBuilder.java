package com.duncpro.jackal;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@NotThreadSafe
public interface StatementBuilder {
    /**
     * Effectively interpolates the given arguments into the SQL statement. The first argument in the list
     * is interpolated into the first parameter and so on. The following argument types are supported {@link String},
     * {@link Integer}, {@code byte[]}, {@link Double}, {@link Long}, {@link Boolean}.
     * @param args an array of arguments to append to the end of the argument list.
     * @throws IndexOutOfBoundsException if the number of arguments passed exceeds the number of parameters
     * within the SQL statement.
     * @throws IllegalArgumentException if an unsupported type is passed as an argument.
     */
    StatementBuilder withArguments(Object... args);

    /**
     * Appends the given argument to the end of the argument list.
     * This is an alias for {@link #withArguments}. See the aforementioned function's declaration
     * for complete documentation.
     */
    default StatementBuilder withArgument(Object arg) {
        return withArguments(arg);
    }

    /**
     * Returns a stream containing the results of the query. Since {@link Stream} is lazy the query will
     * not be executed until a terminal operation is performed.
     * To prevent resource leaks the caller should always close the stream after use.
     * @throws IllegalStateException if one or more parameters are missing arguments.
     */
    Stream<QueryResultRow> query();

    /**
     * Executes an update on the database and returns a {@link CompletableFuture} which completes after the
     * update has finished.
     */
    CompletableFuture<Void> executeUpdate();
}
