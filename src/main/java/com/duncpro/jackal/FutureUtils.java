package com.duncpro.jackal;

import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FutureUtils {
    public static <T> Stream<T> unwrapFutureStream(CompletableFuture<Stream<T>> streamFuture) {
        final Supplier<Spliterator<T>> supplier
                = () -> streamFuture.join().spliterator();

        return StreamSupport.stream(supplier,
                Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE, false);
    }

    public static void unwrapCompletionException(CompletionException e) throws RelationalDatabaseException {
        if (e.getCause() instanceof UncheckedRelationalDatabaseException) {
            throw (RelationalDatabaseException) e.getCause().getCause();
        }
        if (e.getCause() instanceof RuntimeException) {
            throw e;
        }
        throw e;
    }
}
