package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.ExceptionalBehaviorTest;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;

public class JDBCExceptionalBehaviorTest extends ExceptionalBehaviorTest {
    private ExecutorService statementExecutor;

    private ExecutorService transactionE

    @Before
    public void setup() {
        statementExecutor = Executors.newSingleThreadExecutor();
        brokenRelationalDatabase = new DataSourceWrapper(statementExecutor, new BasicDataSource());
    }

    @After
    public void teardown() {
        statementExecutor.shutdownNow();
    }

    @Test
    public void throwsExceptionWithValidCauseUponNewTransaction() {
        final var thrown = super.throwsExceptionUponNewTransaction();
        assertTrue(java.sql.SQLException.class.isAssignableFrom(thrown.getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteUpdate() {
        final var thrown = super.throwsExceptionUponExecuteUpdate();
        assertTrue(java.sql.SQLException.class.isAssignableFrom(thrown.getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteUpdateAsync() {
        final var thrown = super.throwsExceptionUponExecuteUpdateAsync();
        assertTrue(com.duncpro.jackal.SQLException.class.isAssignableFrom(thrown.getCause().getClass()));
        assertTrue(java.sql.SQLException.class.isAssignableFrom(thrown.getCause().getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteQueryAsync() {
        final var thrown = super.throwsExceptionUponExecuteQueryAsync();
        assertTrue(com.duncpro.jackal.SQLException.class.isAssignableFrom(thrown.getCause().getClass()));
        assertTrue(java.sql.SQLException.class.isAssignableFrom(thrown.getCause().getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteQuery() {
        final var thrown = super.throwsExceptionUponExecuteQuery();
        assertTrue(java.sql.SQLException.class.isAssignableFrom(thrown.getCause().getClass()));
    }
}
