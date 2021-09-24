package com.duncpro.jackal;

import org.junit.Assert;

import java.util.Optional;
import java.util.function.Consumer;

public class RollbackTestProcedure implements Consumer<RelationalDatabase>  {
    @Override
    public void accept(RelationalDatabase db) {
        db.prepareStatement("DROP TABLE IF EXISTS dogs;")
                .executeUpdate()
                .join();

        db.prepareStatement("CREATE TABLE dogs (name VARCHAR NOT NULL);")
                .executeUpdate()
                .join();

        db.prepareStatement("INSERT INTO dogs VALUES (?)")
                .withArguments("Cocoa")
                .executeUpdate()
                .join();

        db.runTransaction(t -> {
            t.prepareStatement("DELETE from dogs WHERE name = ?;")
                    .withArguments("Cocoa")
                    .executeUpdate()
                    .join();


            t.rollback().join();
            return null;
        }).join();

        boolean cocoaExists;

        try (final var results = db.prepareStatement("SELECT * FROM dogs;")
                .executeQuery()) {
            cocoaExists = results.map(row -> row.get("name", String.class))
                    .map(Optional::orElseThrow) // name is a NOT NULL column
                    .anyMatch((name) -> name.equals("Cocoa"));
        }

        Assert.assertTrue(cocoaExists);
    }
}
