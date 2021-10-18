package com.duncpro.jackal;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class TypeTestingProcedure implements CommonTestingProcedure {
    @Override
    public void setup(RelationalDatabase db) {
        db.prepareStatement("DROP TABLE IF EXISTS typeTestTable;")
                .startUpdate()
                .join();

        db.prepareStatement("CREATE TABLE typeTestTable (string VARCHAR, int INTEGER, conditional BOOLEAN," +
                " dec DECIMAL);")
                .startUpdate()
                .join();
    }

    @Override
    public void test(RelationalDatabase db) {
        try {
            db.prepareStatement("INSERT INTO typeTestTable VALUES (?, ?, ?, ?);")
                    .withArguments("hello", 1, true, 23.2)
                    .executeUpdate();

            final var result = db.prepareStatement("SELECT * FROM typeTestTable LIMIT 1;")
                    .executeQuery()
                    .findFirst()
                    .orElseThrow();

            assertEquals("hello", result.get("string", String.class)
                    .orElseThrow());
            assertEquals(Integer.valueOf(1), result.get("int", Integer.class)
                    .orElseThrow());
            assertEquals(true, result.get("conditional", Boolean.class)
                    .orElseThrow());
            assertEquals(BigDecimal.valueOf(23.2), result.get("dec", BigDecimal.class)
                    .orElseThrow());
        } catch (RelationalDatabaseException e) {
            throw new AssertionError(e);
        }
    }
}
