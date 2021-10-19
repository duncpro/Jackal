package com.duncpro.jackal;

import java.util.concurrent.CompletionException;

import static org.junit.Assert.*;

public class ExceptionalBehaviorTest {
    protected RelationalDatabase brokenRelationalDatabase = null;

    public Throwable throwsExceptionUponNewTransaction() {
        return assertThrows(RelationalDatabaseException.class,
                () -> brokenRelationalDatabase.startTransaction());
    }

    public Throwable throwsExceptionUponExecuteUpdate() {
        return assertThrows(RelationalDatabaseException.class,
                () -> brokenRelationalDatabase.prepareStatement("").executeUpdate());
    }

    public Throwable throwsExceptionUponExecuteUpdateAsync() {
        return assertThrows(CompletionException.class,
                () -> brokenRelationalDatabase.prepareStatement("").executeUpdateAsync().join());
    }

    public Throwable throwsExceptionUponExecuteQueryAsync() {
        return assertThrows(CompletionException.class,
                () -> brokenRelationalDatabase.prepareStatement("").executeQueryAsync().join());
    }

    public Throwable throwsExceptionUponExecuteQuery() {
        return assertThrows(RelationalDatabaseException.class,
                () -> brokenRelationalDatabase.prepareStatement("").executeQuery());
    }
}
