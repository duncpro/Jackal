package com.duncpro.jackal.jdbc;

import java.sql.SQLException;
import java.util.concurrent.CompletionException;

public class AsyncSQLException extends CompletionException {
    AsyncSQLException(SQLException cause) {
        super(cause);
    }

    public SQLException getSQLException() {
        return (SQLException) getCause();
    }
}
