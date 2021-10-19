package com.duncpro.jackal.aws;

import com.duncpro.jackal.ExceptionalBehaviorTest;
import com.duncpro.jackal.RelationalDatabaseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkException;

import static org.junit.Assert.assertTrue;

public class AuroraServerlessExceptionalBehaviorTest extends ExceptionalBehaviorTest {
    @Before
    public void setup() {
        super.brokenRelationalDatabase = new DefaultAuroraServerlessRelationalDatabase("", "");
    }

    @After
    public void teardown() throws RelationalDatabaseException {
        ((DefaultAuroraServerlessRelationalDatabase) super.brokenRelationalDatabase).close();
    }

    @Test
    public void throwsExceptionWithValidCauseUponNewTransaction() {
        final var thrown = super.throwsExceptionUponNewTransaction();
        assertTrue(SdkException.class.isAssignableFrom(thrown.getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteUpdate() {
        final var thrown = super.throwsExceptionUponExecuteUpdate();
        assertTrue(SdkException.class.isAssignableFrom(thrown.getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteUpdateAsync() {
        final var thrown = super.throwsExceptionUponExecuteUpdateAsync();
        assertTrue(RelationalDatabaseException.class.isAssignableFrom(thrown.getCause().getClass()));
        assertTrue(SdkException.class.isAssignableFrom(thrown.getCause().getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteQueryAsync() {
        final var thrown = super.throwsExceptionUponExecuteQueryAsync();
        assertTrue(RelationalDatabaseException.class.isAssignableFrom(thrown.getCause().getClass()));
        assertTrue(SdkException.class.isAssignableFrom(thrown.getCause().getCause().getClass()));
    }

    @Test
    public void throwsExceptionWithValidCauseUponExecuteQuery() {
        final var thrown = super.throwsExceptionUponExecuteQuery();
        assertTrue(SdkException.class.isAssignableFrom(thrown.getCause().getClass()));
    }
}
