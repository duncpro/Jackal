package com.duncpro.jackal.aws;

import com.duncpro.jackal.ExecuteUpdateTest;
import com.duncpro.jackal.RelationalDatabaseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;

public class AuroraServerlessExecuteUpdateTest extends ExecuteUpdateTest {
    @BeforeClass
    public static void setup() throws IOException {
        db = new TestingAuroraServerlessRelationalDatabase();
    }

    @AfterClass
    public static void teardown() throws RelationalDatabaseException {
        ((DefaultAuroraServerlessRelationalDatabase) db).close();
    }
}
