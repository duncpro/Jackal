package com.duncpro.jackal;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.duncpro.jackal.InterpolatableSQLStatement.sql;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Ignore
public class TransactionTest {
    protected static SQLDatabase db = null;

    @Before
    public void createTables() throws SQLException {
        sql("DROP TABLE IF EXISTS Person;")
                .executeUpdate(db);
        sql("CREATE TABLE IF NOT EXISTS Person (first_name VARCHAR);")
                .executeUpdate(db);
    }

    @Test
    public void commitTransactionTest() throws SQLException {
        try (final var transaction = db.startTransaction()) {
            sql("INSERT INTO Person (first_name) VALUES (?);")
                    .withArguments("Ben")
                    .executeUpdate(transaction);
            transaction.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }

        sql("SELECT first_name FROM Person WHERE first_name = ?;")
                .withArguments("Ben")
                .executeQuery(db)
                .findAny()
                .orElseThrow();
    }

    @Test
    public void implicitRollbackTest() throws SQLException {
        try (final var transaction = db.startTransaction()) {
            sql("INSERT INTO Person (first_name) VALUES (?);")
                    .withArguments("Ben")
                    .executeUpdate(transaction);
//            transaction.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            fail();
        }

        final var wasRolledBack = sql("SELECT first_name FROM Person WHERE first_name = ?;")
                .withArguments("Ben")
                .executeQuery(db)
                .findAny()
                .isEmpty();

        assertTrue(wasRolledBack);
    }
}
