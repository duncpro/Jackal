package com.duncpro.rds.data;

import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {
    public static <T> Stream<T> unwrapStream(CompletableFuture<Stream<T>> streamFuture) {
        final Supplier<Spliterator<T>> supplier
                = () -> streamFuture.join().spliterator();

        return StreamSupport.stream(supplier,
                Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE, false);
    }
}
