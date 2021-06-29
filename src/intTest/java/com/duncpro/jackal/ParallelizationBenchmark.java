package com.duncpro.jackal;

import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ParallelizationBenchmark implements Consumer<AsyncDatabase> {

    @Override
    public void accept(AsyncDatabase db) {
        final var rowCount = 100;
        System.out.println("Number of rows inserted = " + rowCount);
        final var sampleData = new Random().ints(rowCount).toArray();

        setup(db);

        parallel(Arrays.stream(sampleData), db);
        sequential(Arrays.stream(sampleData), db);
    }

    private void setup(AsyncDatabase db) {
        db.prepareStatement("DROP TABLE IF EXISTS nums;")
                .executeUpdate()
                .join();

        db.prepareStatement("CREATE TABLE nums (n INTEGER);")
            .executeUpdate()
            .join();
    }

    private void sequential(IntStream sampleData, AsyncDatabase db) {
        final var startTime = System.currentTimeMillis();

        sampleData
                .forEach(n -> db.prepareStatement("INSERT INTO nums VALUES (?);")
                        .withArgument(n)
                        .executeUpdate()
                        .join()
                );

        System.out.println("Sequential: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void parallel(IntStream sampleData, AsyncDatabase db) {
        final var startTime = System.currentTimeMillis();

        sampleData.parallel()
                .forEach(n -> db.prepareStatement("INSERT INTO nums VALUES (?);")
                    .withArgument(n)
                    .executeUpdate()
                    .join()
                );

        System.out.println("Parallel: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
