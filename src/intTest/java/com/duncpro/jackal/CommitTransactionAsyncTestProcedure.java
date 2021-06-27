package com.duncpro.jackal;

import org.junit.Assert;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommitTransactionAsyncTestProcedure implements Consumer<AsyncDatabase> {
    @Override
    public void accept(AsyncDatabase db) {
        final var expected = Set.of("Will", "Allison", "Madison");

        db.commitTransactionAsync(new CreatePeopleTransaction(expected)).join();

        Set<String> actual;

        try (final var results = db.prepareStatement("SELECT first_name FROM people;")
                .executeQuery()) {
            actual = results.map(row -> row.getString("first_name"))
                    .collect(Collectors.toSet());
        }
        Assert.assertEquals(expected, actual);
    }
}
