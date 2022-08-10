package com.duncpro.jackal.util;

import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class Throwables {
    /**
     * Returns the "value" of the given throwable. This function discards any {@link CompletionException} wrapper
     * exception, returning the true cause of the error. If the given {@link Throwable} does not have any
     * {@link CompletionException} wrapper, or then the input is simply returned. If the given {@link CompletionException}
     * does not have a cause, then the {@link CompletionException} itself is returned.
     */
    public static Throwable unwrapCompletionException(Throwable e) {
        if (e instanceof CompletionException && e.getCause() != null) return e.getCause();
        return e;
    }

    /**
     * Unwraps the given {@link CompletionException} if possible, throwing the {@link Error}s, {@link RuntimeException}s,
     * or {@link T}s which the {@link CompletionException} encapsulates. If the cause of the {@link CompletionException}
     * is none of the aforementioned types, then the {@link CompletionException} itself is thrown.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void unwrapAndThrow(CompletionException e, Class<T> type) throws T {
        final var cause = e.getCause();
        if (cause == null) throw e;
        if (type.isAssignableFrom(cause.getClass())) throw (T) cause;
        if (cause instanceof RuntimeException) throw (RuntimeException) cause;
        if (cause instanceof Error) throw (Error) cause;
        throw e;
    }

    @SuppressWarnings("unchecked")
    public static <I extends Throwable, O extends Throwable> void unwrapAndThrow(CompletionException e,
                                                                                 Class<I> input,
                                                                                 Function<I, O> map) throws O {
        final var cause = e.getCause();
        if (cause == null) throw e;
        if (input.isAssignableFrom(cause.getClass())) throw map.apply((I) cause);
        if (cause instanceof RuntimeException) throw (RuntimeException) cause;
        if (cause instanceof Error) throw (Error) cause;
        throw e;
    }
}
