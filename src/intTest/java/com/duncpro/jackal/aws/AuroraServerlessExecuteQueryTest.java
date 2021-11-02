package com.duncpro.jackal.aws;

import com.duncpro.jackal.ExecuteQueryTest;
import com.duncpro.jackal.SQLException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;

public class AuroraServerlessExecuteQueryTest extends ExecuteQueryTest {
    @BeforeClass
    public static void setup() throws IOException {
        db = new TestingAuroraServerlessRelationalDatabase();
    }

    @AfterClass
    public static void teardown() throws SQLException {
        ((DefaultAuroraServerlessDatabase) db).close();
    }
}
