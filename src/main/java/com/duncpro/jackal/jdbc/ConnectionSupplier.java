package com.duncpro.jackal.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionSupplier {
    /**
     * Attempts to open a connection to the database.
     * If a connection can be established then a new {@link Connection} object is returned.
     * If a connection cannot be established than {@link SQLException} is thrown.
     */
    Connection get() throws SQLException;
}
