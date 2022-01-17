package com.duncpro.jackal;

import com.duncpro.jackal.aws.AuroraServerlessSQLExecutor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.stream.Collectors;

import static com.duncpro.jackal.InterpolatableSQLStatement.sql;
import static org.junit.Assert.*;

@Ignore
public class ExecuteQueryTest {
    protected static SQLDatabase db = null;

    @Before
    public void createTables() throws SQLException {
        sql("DROP TABLE IF EXISTS Person;").executeUpdate(db);
        sql("CREATE TABLE IF NOT EXISTS Person (first_name VARCHAR);").executeUpdate(db);
        sql("INSERT INTO Person (first_name) VALUES (?);")
                .withArguments("Duncan")
                .executeUpdate(db);
    }

    @Test
    public void executeQueryTest() throws SQLException {
        final var results = sql("SELECT first_name FROM Person WHERE first_name = (?)")
                .withArguments("Duncan")
                .executeQuery(db)
                .collect(Collectors.toList());

        assertEquals(1, results.size());
        assertEquals("Duncan", results.get(0).get("first_name", String.class).orElseThrow());
    }

    @Test
    public void executeQueryAsyncTest() {
        final var results = sql("SELECT first_name FROM Person WHERE first_name = (?)")
                .withArguments("Duncan")
                .executeQueryAsync(db)
                .join()
                .collect(Collectors.toList());

        assertEquals(1, results.size());
        assertEquals("Duncan", results.get(0).get("first_name", String.class).orElseThrow());
    }

    @Test
    public void executeQueryIncrementallyTest() {
        if (db.getExecutor() instanceof AuroraServerlessSQLExecutor) return;

        try (final var results = sql("SELECT first_name FROM Person WHERE first_name = (?)")
                .withArguments("Duncan")
                .executeQueryIncrementally(db)) {
            final var cachedResults = results.collect(Collectors.toList());
            assertEquals(1, cachedResults.size());
            assertEquals("Duncan", cachedResults.get(0).get("first_name", String.class).orElseThrow());
        }
    }
}
