package com.duncpro.jackal;

import java.math.BigDecimal;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class TypeTestingProcedure implements Function<AsyncDatabase, Void> {
    @Override
    public Void apply(AsyncDatabase db) {
        db.prepareStatement("DROP TABLE IF EXISTS typeTestTable;")
                .executeUpdate()
                .join();

        db.prepareStatement("CREATE TABLE typeTestTable (string VARCHAR, int INTEGER, conditional BOOLEAN," +
                " dec DECIMAL);")
                .executeUpdate()
                .join();

        db.prepareStatement("INSERT INTO typeTestTable VALUES (?, ?, ?, ?);")
                .withArguments("hello", 1, true, 23.2)
                .executeUpdate()
                .join();

        final var result = db.prepareStatement("SELECT * FROM typeTestTable;")
                .query()
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

        return null;
    }
}
