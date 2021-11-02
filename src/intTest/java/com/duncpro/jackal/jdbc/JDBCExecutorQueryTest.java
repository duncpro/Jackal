package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.ExecuteQueryTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;

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
