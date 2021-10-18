package com.duncpro.jackal;

import static org.junit.Assert.assertTrue;

public class CommitTransactionTestingProcedure implements CommonTestingProcedure {
    @Override
    public void setup(RelationalDatabase db) {
        db.prepareStatement("DROP TABLE IF EXISTS Person;")
                .startUpdate()
                .join();

        db.prepareStatement("CREATE TABLE IF NOT EXISTS Person (first_name VARCHAR);")
                .startUpdate()
                .join();
    }

    @Override
    public void test(RelationalDatabase db) {
        try (final var transaction = db.startTransaction()) {
            transaction.prepareStatement("INSERT INTO Person (first_name) VALUES (?)")
                    .withArgument("Duncan")
                    .executeUpdate();
            transaction.commit();
        } catch (RelationalDatabaseException e) {
            throw new AssertionError(e);
        }
        assertTransactionCommitted(db);
    }

    private void assertTransactionCommitted(RelationalDatabase db) {
        final boolean didInsertRow;
        try {
            didInsertRow = db.prepareStatement("SELECT FROM Person WHERE first_name = ? LIMIT 1;")
                    .withArgument("Duncan")
                    .executeQuery()
                    .findAny()
                    .isPresent();
        } catch (RelationalDatabaseException e) {
            throw new AssertionError(e);
        }

        assertTrue(didInsertRow);
    }
}
