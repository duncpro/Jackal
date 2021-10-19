package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.ExecuteQueryTest;
import com.duncpro.jackal.RelationalDatabaseException;
import com.duncpro.jackal.aws.DefaultAuroraServerlessRelationalDatabase;
import com.duncpro.jackal.aws.TestingAuroraServerlessRelationalDatabase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.sql.SQLException;

public class JDBCExecutorQueryTest extends ExecuteQueryTest {
    @BeforeClass
    public static void setup() throws SQLException {
        db = new H2RelationalDatabase();
    }

    @AfterClass
    public static void teardown() throws Exception {
        ((H2RelationalDatabase) db).close();
    }
}
