package com.duncpro.jackal;

import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class Throwables {
    public static Throwable unwrapCompletionException(Throwable e) {
        if (e instanceof CompletionException && e.getCause() != null) return e.getCause();
        return e;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void unwrapAndThrow(CompletionException e, Class<T> type) throws T {
        final var cause = e.getCause();
        if (cause == null) throw e;
        if (cause instanceof RuntimeException) throw (RuntimeException) cause;
        if (cause instanceof Error) throw (Error) cause;
        if (type.isAssignableFrom(cause.getClass())) throw (T) cause;
        throw e;
    }

    @SuppressWarnings("unchecked")
    public static <I extends Throwable, O extends Throwable> void unwrapAndThrow(CompletionException e,
                                                                                 Class<I> input,
                                                                                 Function<I, O> map) throws O {
        final var cause = e.getCause();
        if (cause == null) throw e;
        if (cause instanceof RuntimeException) throw (RuntimeException) cause;
        if (cause instanceof Error) throw (Error) cause;
        if (input.isAssignableFrom(cause.getClass())) throw map.apply((I) cause);
        throw e;
    }
}
