package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.ExceptionalBehaviorTest;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.duncpro.jackal.SQLException;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;

public class JDBCExceptionalBehaviorTest extends ExceptionalBehaviorTest {
    @Before
    public void setup() {
        brokenRelationalDatabase = new DataSourceWrapper(Executors.newSingleThreadExecutor(), new BasicDataSource());
    }

    @After
    public void teardown() {
        ((DataSourceWrapper) brokenRelationalDatabase).taskExecutor.shutdownNow();
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
