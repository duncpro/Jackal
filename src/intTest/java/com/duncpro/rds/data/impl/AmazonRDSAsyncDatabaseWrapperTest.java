package com.duncpro.rds.data.impl;

import com.duncpro.rds.data.AsyncDatabase;
import com.duncpro.rds.data.CreatePeopleTransaction;
import lombok.Value;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    public void testTransaction() {
        final var expected = Set.of("Will", "Allison", "Madison");

        db.commitTransactionAsync(new CreatePeopleTransaction(expected)).join();

        final var actual = db.prepareStatement("SELECT first_name FROM people;")
                .executeQuery()
                .map(row -> row.getString("first_name"))
                .collect(Collectors.toSet());

        Assert.assertEquals(expected, actual);
    }
}
