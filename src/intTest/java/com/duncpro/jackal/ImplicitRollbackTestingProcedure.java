package com.duncpro.jackal;

import org.junit.Assert;

import static org.junit.Assert.assertTrue;

public class ImplicitRollbackTestingProcedure implements CommonTestingProcedure {
    @Override
    public void setup(RelationalDatabase db) {
        db.prepareStatement("DROP TABLE IF EXISTS Person;")
                .startUpdate()
                .join();

        db.prepareStatement("CREATE TABLE Person (first_name VARCHAR);")
                .startUpdate()
                .join();
    }

    @Override
    public void test(RelationalDatabase db) {
        class SomeException extends Exception {}

        try (final var transaction = db.startTransaction()) {
            transaction.prepareStatement("INSERT INTO Person (first_name) VALUES (?);")
                    .withArgument("Duncan")
                    .executeUpdate();

            // Intentionally throwing exception to make sure the transaction is rolled back as expected.
            // noinspection ConstantConditions
            if (true) throw new SomeException();

            transaction.commit();
        } catch (RelationalDatabaseException e) {
            throw new AssertionError(e);
        } catch (SomeException e) {
            assertRowWasNotInserted(db);
        }
    }

    private void assertRowWasNotInserted(RelationalDatabase db) {
        boolean wasNotCreated = db.prepareStatement("SELECT first_name FROM Person WHERE first_name = ?;")
                .withArgument("Duncan")
                .executeQuery()
                .findAny()
                .isEmpty();

        assertTrue(wasNotCreated);
    }
}
