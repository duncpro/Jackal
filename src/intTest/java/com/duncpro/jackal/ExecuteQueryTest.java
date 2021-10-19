package com.duncpro.jackal;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.junit.Assert.*;

@Ignore
public class ExecuteQueryTest {
    protected static RelationalDatabase db = null;

    @Before
    public void createTables() throws RelationalDatabaseException {
        db.prepareStatement("DROP TABLE IF EXISTS Person;").executeUpdate();
        db.prepareStatement("CREATE TABLE IF NOT EXISTS Person (first_name VARCHAR);").executeUpdate();
        db.prepareStatement("INSERT INTO Person (first_name) VALUES (?);")
                .withArguments("Duncan")
                .executeUpdate();
    }

    @Test
    public void executeQueryTest() throws RelationalDatabaseException {
        final var results = db.prepareStatement("SELECT first_name FROM Person WHERE first_name = (?)")
                .withArguments("Duncan")
                .executeQuery()
                .collect(Collectors.toList());

        assertEquals(1, results.size());
        assertEquals("Duncan", results.get(0).get("first_name", String.class).orElseThrow());
    }

    @Test
    public void executeQueryAsyncTest() {
        final var results = db.prepareStatement("SELECT first_name FROM Person WHERE first_name = (?)")
                .withArguments("Duncan")
                .executeQueryAsync()
                .join()
                .collect(Collectors.toList());

        assertEquals(1, results.size());
        assertEquals("Duncan", results.get(0).get("first_name", String.class).orElseThrow());
    }
}
