package com.duncpro.jackal;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ParallelizationBenchmark implements CommonTestingProcedure {
    public void setup(RelationalDatabase db) {
        db.prepareStatement("DROP TABLE IF EXISTS nums;")
                .startUpdate()
                .join();

        db.prepareStatement("CREATE TABLE nums (n INTEGER);")
            .startUpdate()
            .join();
    }

    @Override
    public void test(RelationalDatabase db) {
        final var rowCount = 100;
        System.out.println("Number of rows inserted = " + rowCount);
        final var sampleData = new Random().ints(rowCount).toArray();

        setup(db);

        parallel(Arrays.stream(sampleData), db);
        sequential(Arrays.stream(sampleData), db);
    }

    private void sequential(IntStream sampleData, RelationalDatabase db) {
        final var startTime = System.currentTimeMillis();

        sampleData.forEach(n -> db.prepareStatement("INSERT INTO nums VALUES (?);")
                .withArgument(n)
                .startUpdate()
                .join()
        );

        System.out.println("Sequential: " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private void parallel(IntStream sampleData, RelationalDatabase db) {
        final var startTime = System.currentTimeMillis();

        sampleData.parallel()
                .forEach(n -> db.prepareStatement("INSERT INTO nums VALUES (?);")
                    .withArgument(n)
                    .startUpdate()
                    .join()
                );

        System.out.println("Parallel: " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
