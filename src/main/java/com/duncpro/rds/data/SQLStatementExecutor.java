package com.duncpro.rds.data;

interface SQLStatementExecutor {
    /**
     * Creates a new standalone statement which is automatically committed upon completion.
     * @param parameterizedSQL a JDBC-like parameterized SQL query.
     */
    StatementBuilder prepareStatement(String parameterizedSQL);
}
