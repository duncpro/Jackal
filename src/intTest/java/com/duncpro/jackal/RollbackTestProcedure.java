package com.duncpro.jackal;

import org.junit.Assert;

import java.util.function.Consumer;

public class RollbackTestProcedure implements Consumer<AsyncDatabase>  {
    @Override
    public void accept(AsyncDatabase db) {
        db.prepareStatement("DROP TABLE IF EXISTS dogs;")
                .executeUpdate()
                .join();

        db.prepareStatement("CREATE TABLE dogs (name VARCHAR);")
                .executeUpdate()
                .join();

        db.prepareStatement("INSERT INTO dogs VALUES (?)")
                .setString(0, "Cocoa")
                .executeUpdate()
                .join();

        db.runTransactionAsync(t -> {
            t.prepareStatement("DELETE from dogs WHERE name = ?;")
                    .setString(0, "Cocoa");
            t.rollback().join();
            return null;
        }).join();

        final var cocoaExists = db.prepareStatement("SELECT * FROM dogs;")
                .executeQuery()
                .map(row -> row.getString("name"))
                .anyMatch((name) -> name.equals("Cocoa"));

        Assert.assertTrue(cocoaExists);
    }
}
