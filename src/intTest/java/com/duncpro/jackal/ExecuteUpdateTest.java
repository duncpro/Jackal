package com.duncpro.jackal;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CompletionException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore
public class ExecuteUpdateTest {
    protected static RelationalDatabase db = null;

    @Before
    public void createTables() throws RelationalDatabaseException {
        db.prepareStatement("DROP TABLE IF EXISTS Person;").executeUpdate();
        db.prepareStatement("CREATE TABLE IF NOT EXISTS Person (first_name VARCHAR);").executeUpdate();
    }

    @Test
    public void executeUpdateTest() throws RelationalDatabaseException {
        try {
            db.prepareStatement("INSERT INTO Person (first_name) VALUES (?);")
                    .withArguments("Duncan")
                    .executeUpdate();
        } catch (RelationalDatabaseException e) {
            e.printStackTrace();
            fail("executeUpdate call failed");
        }

        final boolean didInsert = db.prepareStatement("SELECT first_name FROM Person WHERE first_name = (?)")
                .withArguments("Duncan")
                .executeQuery()
                .findAny()
                .isPresent();

        assertTrue(didInsert);
    }

    @Test
    public void executeUpdateAsyncTest() throws RelationalDatabaseException {
        try {
            db.prepareStatement("INSERT INTO Person (first_name) VALUES (?);")
                    .withArguments("Duncan")
                    .executeUpdateAsync()
                    .join();
        } catch (CompletionException e) {
            e.printStackTrace();
            fail("executeUpdateAsync join failed");
        }

        final boolean didInsert = db.prepareStatement("SELECT first_name FROM Person WHERE first_name = (?)")
                .withArguments("Duncan")
                .executeQuery()
                .findAny()
                .isPresent();

        assertTrue(didInsert);
    }
}
