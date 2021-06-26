package com.duncpro.jackal;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@NotThreadSafe
public interface StatementBuilder {
    /**
     * Appends the given arguments to the end of the argument list.
     * Supported data types are {@link String}, {@link Long}, {@link Boolean}.
     * @param args an array of arguments to append to the end of the argument list.
     * @throws IndexOutOfBoundsException if the number of arguments passed exceeds the number of parameters
     * within the SQL statement.
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

    Stream<QueryResultRow> executeQuery();

    CompletableFuture<Void> executeUpdate();
}
