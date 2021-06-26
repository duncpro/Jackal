package com.duncpro.rds.data.impl;

import com.duncpro.rds.data.AsyncDatabase;
import lombok.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
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

    @Value
    private static class AWSResources {
        String dbArn;
        String secretArn;
    }

    private Optional<AWSResources> loadAWSResourcesFromFileSystem() throws IOException {
        final var propertiesFs = getClass().getResourceAsStream("/rds-instance.properties");

        if (propertiesFs == null) return Optional.empty();

        final var properties = new Properties();

        try (final var reader = new InputStreamReader(propertiesFs)) {
            properties.load(reader);
        }
        return Optional.of(
                new AWSResources(
                        properties.getProperty("dbArn"),
                        properties.getProperty("secretArn")
                )
        );
    }

    private AWSResources loadAWSResourcesFromEnvironment() {
        return new AWSResources(
                System.getenv("AWS_DB_ARN"),
                System.getenv("AWS_DB_SECRET_ARN")
        );
    }

    @Before
    public void initializeRDSWrapper() throws IOException {
        final var awsResources = loadAWSResourcesFromFileSystem()
                .orElseGet(this::loadAWSResourcesFromEnvironment);

        db = new AmazonRDSAsyncDatabaseWrapper(rdsDataClient, awsResources.dbArn, awsResources.secretArn,
                transactionExecutor);
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
                    .findFirst()
                    .orElseThrow(AssertionError::new); // We just inserted a record.
        });

        assertEquals("hello", retrievedValue.join().getString("col_a"));
        assertEquals(100L, retrievedValue.join().getLong("col_b"));
        assertEquals(true, retrievedValue.join().getBoolean("col_c"));
    }
}
