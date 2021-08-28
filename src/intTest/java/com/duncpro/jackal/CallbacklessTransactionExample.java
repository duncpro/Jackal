package com.duncpro.jackal;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CallbacklessTransactionExample implements Consumer<AsyncDatabase> {
    @Override
    public void accept(AsyncDatabase database) {
        final var transaction = database.startTransaction();

        final var processCompleted = getUserNames(transaction)
                .map(userName -> resetPoints(userName, transaction))
                .flatMap(Mono::fromFuture)
                .then()
                .doOnSuccess($ -> transaction.commit())
                .doOnCancel(transaction::rollback)
                .doOnError($ -> transaction.rollback())
                .then().toFuture();

        processCompleted.join();
    }

    private CompletableFuture<Void> resetPoints(String userName, AsyncDatabaseTransaction transaction) {
        return transaction.prepareStatement("DELETE FROM users WHERE user_name = ?")
                .withArguments(userName)
                .executeUpdate();
    }

    private Flux<String> getUserNames(AsyncDatabaseTransaction transaction) {
        final var firstNamesStream = transaction.prepareStatement("SELECT * FROM users;")
                .executeQuery()
                .map(row -> row.get("user_name", String.class))
                .map(userName -> userName.orElseThrow(AssertionError::new));

        return Flux.fromStream(firstNamesStream);
    }
}
