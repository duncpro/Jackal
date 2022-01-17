package com.duncpro.jackal;

import static java.util.Objects.requireNonNull;

public class UncheckedSQLException extends RuntimeException {
    public UncheckedSQLException(Exception cause) {
        super(cause);
    }
}
