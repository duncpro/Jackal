package com.duncpro.jackal;

import org.intellij.lang.annotations.Language;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
interface SQLStatementExecutor {
    /**
     * Creates a new standalone statement which is automatically committed upon completion.
     * @param parameterizedSQL a JDBC-like parameterized SQL query.
     */
    StatementBuilder prepareStatement(@Language("SQL") String parameterizedSQL);
}
