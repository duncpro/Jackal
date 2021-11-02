package com.duncpro.jackal;

import java.util.concurrent.CompletionException;

import static com.duncpro.jackal.InterpolatableSQLStatement.sql;
import static org.junit.Assert.*;

public class ExceptionalBehaviorTest {
    protected SQLDatabase brokenRelationalDatabase = null;

    public Throwable throwsExceptionUponNewTransaction() {
        return assertThrows(SQLException.class,
                () -> brokenRelationalDatabase.startTransaction());
    }

    public Throwable throwsExceptionUponExecuteUpdate() {
        return assertThrows(SQLException.class,
                () -> sql("").executeUpdate(brokenRelationalDatabase));
    }

    public Throwable throwsExceptionUponExecuteUpdateAsync() {
        return assertThrows(CompletionException.class,
                () -> sql("").executeUpdateAsync(brokenRelationalDatabase).join());
    }

    public Throwable throwsExceptionUponExecuteQueryAsync() {
        return assertThrows(CompletionException.class,
                () -> sql("").executeQueryAsync(brokenRelationalDatabase).join());
    }

    public Throwable throwsExceptionUponExecuteQuery() {
        return assertThrows(SQLException.class,
                () -> sql("").executeQuery(brokenRelationalDatabase));
    }
}
