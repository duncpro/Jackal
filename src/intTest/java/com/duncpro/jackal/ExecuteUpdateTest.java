package com.duncpro.jackal;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CompletionException;

import static com.duncpro.jackal.InterpolatableSQLStatement.sql;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore
public class ExecuteUpdateTest {
    protected static SQLDatabase db = null;

    @Before
    public void createTables() throws SQLException {
        sql("DROP TABLE IF EXISTS Person;").executeUpdate(db);
        sql("CREATE TABLE IF NOT EXISTS Person (first_name VARCHAR);").executeUpdate(db);
    }

    @Test
    public void executeUpdateTest() throws SQLException {
        try {
            sql("INSERT INTO Person (first_name) VALUES (?);")
                    .withArguments("Duncan")
                    .executeUpdate(db);
        } catch (SQLException e) {
            e.printStackTrace();
            fail("executeUpdate call failed");
        }

        final boolean didInsert = sql("SELECT first_name FROM Person WHERE first_name = (?)")
                .withArguments("Duncan")
                .executeQuery(db)
                .findAny()
                .isPresent();

        assertTrue(didInsert);
    }

    @Test
    public void executeUpdateAsyncTest() throws SQLException {
        try {
            sql("INSERT INTO Person (first_name) VALUES (?);")
                    .withArguments("Duncan")
                    .executeUpdateAsync(db)
                    .join();
        } catch (CompletionException e) {
            e.printStackTrace();
            fail("executeUpdateAsync join failed");
        }

        final boolean didInsert = sql("SELECT first_name FROM Person WHERE first_name = (?)")
                .withArguments("Duncan")
                .executeQuery(db)
                .findAny()
                .isPresent();

        assertTrue(didInsert);
    }
}
