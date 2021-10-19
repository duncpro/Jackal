package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.ExceptionalBehaviorTest;
import com.duncpro.jackal.RelationalDatabaseException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkException;

import java.sql.SQLException;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;

public class JDBCExceptionalBehaviorTest extends ExceptionalBehaviorTest {
    @Before
    public void setup() {
        brokenRelationalDatabase = new DataSourceWrapper(new BasicDataSource(), Executors.newSingleThreadExecutor());
    }

    @After
    public void teardown() {
        ((DataSourceWrapper) brokenRelationalDatabase).executor.shutdownNow();
    }

    @Test
    public void throwsExceptionWithValidCauseUponNewTransaction() {
        final var thrown = super.throwsExceptionUponNewTransaction();
        assertTrue(SQLException.class.isAssignableFrom(thrown.getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteUpdate() {
        final var thrown = super.throwsExceptionUponExecuteUpdate();
        assertTrue(SQLException.class.isAssignableFrom(thrown.getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteUpdateAsync() {
        final var thrown = super.throwsExceptionUponExecuteUpdateAsync();
        assertTrue(RelationalDatabaseException.class.isAssignableFrom(thrown.getCause().getClass()));
        assertTrue(SQLException.class.isAssignableFrom(thrown.getCause().getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteQueryAsync() {
        final var thrown = super.throwsExceptionUponExecuteQueryAsync();
        assertTrue(RelationalDatabaseException.class.isAssignableFrom(thrown.getCause().getClass()));
        assertTrue(SQLException.class.isAssignableFrom(thrown.getCause().getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteQuery() {
        final var thrown = super.throwsExceptionUponExecuteQuery();
        assertTrue(SQLException.class.isAssignableFrom(thrown.getCause().getClass()));
    }
}
