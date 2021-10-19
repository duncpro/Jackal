package com.duncpro.jackal;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore
public class TransactionTest {
    protected static RelationalDatabase db = null;

    @Before
    public void createTables() throws RelationalDatabaseException {
        db.prepareStatement("DROP TABLE IF EXISTS Person;").executeUpdate();
        db.prepareStatement("CREATE TABLE IF NOT EXISTS Person (first_name VARCHAR);").executeUpdate();
    }

    @Test
    public void commitTransactionTest() throws RelationalDatabaseException {
        try (final var transaction = db.startTransaction()) {
            transaction.prepareStatement("INSERT INTO Person (first_name) VALUES (?);")
                    .withArguments("Ben")
                    .executeUpdate();
            transaction.commit();
        } catch (RelationalDatabaseException e) {
            e.printStackTrace();
            fail();
        }

        db.prepareStatement("SELECT first_name FROM Person WHERE first_name = ?;")
                .withArguments("Ben")
                .executeQuery()
                .findAny()
                .orElseThrow();
    }

    @Test
    public void implicitRollbackTest() throws RelationalDatabaseException {
        try (final var transaction = db.startTransaction()) {
            transaction.prepareStatement("INSERT INTO Person (first_name) VALUES (?);")
                    .withArguments("Ben")
                    .executeUpdate();
//            transaction.commit();
        } catch (RelationalDatabaseException e) {
            e.printStackTrace();
            fail();
        }

        final var wasRolledBack = db.prepareStatement("SELECT first_name FROM Person WHERE first_name = ?;")
                .withArguments("Ben")
                .executeQuery()
                .findAny()
                .isEmpty();

        assertTrue(wasRolledBack);
    }
}
