package com.duncpro.rds.data.impl;

import com.duncpro.rds.data.AsyncDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class AmazonRDSAsyncDatabaseWrapperTest {

    final ExecutorService transactionExecutor = Executors.newCachedThreadPool();

    @After
    public void shutdownExecutor() {
        transactionExecutor.shutdown();
    }

    private final RdsDataAsyncClient rdsDataClient = RdsDataAsyncClient.create();
    @After
    public void shutdownRdsClient() {
        rdsDataClient.close();
    }

    private AsyncDatabase db;

    @Before
    public void initializeRDSWrapper() throws IOException {
        final var properties = new Properties();
        final var propertiesFs = getClass().getResourceAsStream("/rds-instance.properties");
        assert propertiesFs != null;
        try (final var reader = new InputStreamReader(propertiesFs)) {
            properties.load(reader);
        }

        final var dbArn = properties.getProperty("dbArn");
        final var secretArn = properties.getProperty("secretArn");

        db = new AmazonRDSAsyncDatabaseWrapper(rdsDataClient, dbArn, secretArn, transactionExecutor);
    }

    @Test
    public void selectRecordTransaction() {
        final var retrievedValue = db.commitTransactionAsync(transaction -> {
            transaction.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS table_b (col_a varchar, col_b bigint, col_c boolean);")
                    .executeUpdate()
                    .join();

            transaction.prepareStatement("INSERT INTO table_b VALUES (?, ?, ?);")
                    .setString(0, "hello")
                    .setLong(1, 100L)
                    .setBoolean(2, true)
                    .executeUpdate()
                    .join();

            return transaction.prepareStatement("SELECT * FROM table_b;")
                    .executeQuery()
                    .join()
                    .toRowList();
        });

        assertEquals("hello", retrievedValue.join().get(0).get("col_a"));
        assertEquals(100L, retrievedValue.join().get(0).get("col_b"));
        assertEquals(true, retrievedValue.join().get(0).get("col_c"));
    }
}
