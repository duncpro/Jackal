package com.duncpro.jackal;

/**
 * A {@link RuntimeException} which wraps a {@link RelationalDatabaseException}.
 * The enclosed {@link RelationalDatabaseException} should be unwrapped as soon as possible via
 * {@link #getCause()} and rethrown.
 */
public class UncheckedRelationalDatabaseException extends RuntimeException {
    public UncheckedRelationalDatabaseException(RelationalDatabaseException cause) {
        super(cause);
    }

    @Override
    public RelationalDatabaseException getCause() {
        return (RelationalDatabaseException) super.getCause();
    }
}
