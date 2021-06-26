package com.duncpro.jackal;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CreatePeopleTransaction implements Function<AsyncDatabaseTransaction, Void> {
    private final Collection<String> firstNames;

    public CreatePeopleTransaction(Collection<String> firstNames) {
        this.firstNames = firstNames;
    }

    @Override
    public Void apply(AsyncDatabaseTransaction db) {
        db.prepareStatement("DROP TABLE IF EXISTS people")
                .executeUpdate()
                .join();

        db.prepareStatement("CREATE TABLE people (first_name VARCHAR);")
                .executeUpdate()
                .join();

        firstNames.stream()
                .map(firstName ->
                        db.prepareStatement("INSERT INTO people VALUES (?);")
                                .withArguments(firstName)
                                .executeUpdate()
                )
                .forEach(CompletableFuture::join);

        return null;
    }
}
