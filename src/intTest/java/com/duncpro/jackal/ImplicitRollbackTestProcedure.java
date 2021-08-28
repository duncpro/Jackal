package com.duncpro.jackal;

import org.junit.Assert;

import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public class ImplicitRollbackTestProcedure implements Consumer<AsyncDatabase> {
    @Override
    public void accept(AsyncDatabase db) {
        db.prepareStatement("DROP TABLE IF EXISTS colors;")
                .executeUpdate()
                .join();

        db.prepareStatement("CREATE TABLE colors (name VARCHAR);")
                .executeUpdate()
                .join();

        final var transactionResult = db.commitTransactionAsync(t -> {
            t.prepareStatement("INSERT INTO colors VALUES (?)")
                    .withArguments("red")
                    .executeUpdate()
                    .join();

            throw new CompletionException(new Exception("Something went wrong"));
        });

        try {
            transactionResult.join();
            Assert.fail(); // Exception was not propagated
        } catch (CompletionException e) {
            // Make sure the right exception was propagated
            Assert.assertEquals("Something went wrong", e.getCause().getMessage());
        }

        long colorCount;

        try (final var colorResultStream = db.prepareStatement("SELECT * FROM colors WHERE name = ?")
                .withArguments("red")
                .executeQuery()) {

            colorCount = colorResultStream.count();
        }

        Assert.assertEquals(0, colorCount); // red was not added
    }
}
