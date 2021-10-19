package com.duncpro.jackal.aws;

import lombok.Value;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Properties;

public class TestingAuroraServerlessRelationalDatabase extends DefaultAuroraServerlessRelationalDatabase {
    public TestingAuroraServerlessRelationalDatabase() throws IOException {
        super(getIdentity().dbArn, getIdentity().secretArn);
    }

    @Value
    private static class TestingDatabaseIdentity {
        String dbArn;
        String secretArn;
    }

    private static TestingDatabaseIdentity getIdentity() throws IOException {
        return loadAWSResourcesFromFileSystem()
                .orElseGet(TestingAuroraServerlessRelationalDatabase::loadAWSResourcesFromEnvironment);
    }

    private static Optional<TestingDatabaseIdentity> loadAWSResourcesFromFileSystem() throws IOException {
        final var propertiesFs = TestingAuroraServerlessRelationalDatabase.class
                .getResourceAsStream("/rds-instance.properties");

        if (propertiesFs == null) return Optional.empty();

        final var properties = new Properties();

        try (final var reader = new InputStreamReader(propertiesFs)) {
            properties.load(reader);
        }
        return Optional.of(
                new TestingDatabaseIdentity(
                        properties.getProperty("dbArn"),
                        properties.getProperty("secretArn")
                )
        );
    }

    private static TestingDatabaseIdentity loadAWSResourcesFromEnvironment() {
        return new TestingDatabaseIdentity(
                System.getenv("AWS_DB_ARN"),
                System.getenv("AWS_DB_SECRET_ARN")
        );
    }
}
