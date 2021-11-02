package com.duncpro.jackal.aws;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Properties;

public class TestingAuroraServerlessRelationalDatabase extends DefaultAuroraServerlessDatabase {
    public TestingAuroraServerlessRelationalDatabase() throws IOException {
        super(getCredentials());
    }

    private static AuroraServerlessCredentials getCredentials() throws IOException {
        return loadCredentialsFromFileSystem()
                .orElseGet(TestingAuroraServerlessRelationalDatabase::loadCredentialsFromEnvironment);
    }

    private static Optional<AuroraServerlessCredentials> loadCredentialsFromFileSystem() throws IOException {
        final var propertiesFs = TestingAuroraServerlessRelationalDatabase.class
                .getResourceAsStream("/rds-instance.properties");

        if (propertiesFs == null) return Optional.empty();

        final var properties = new Properties();

        try (final var reader = new InputStreamReader(propertiesFs)) {
            properties.load(reader);
        }
        return Optional.of(
                new AuroraServerlessCredentials(
                        properties.getProperty("dbArn"),
                        properties.getProperty("secretArn")
                )
        );
    }

    private static AuroraServerlessCredentials loadCredentialsFromEnvironment() {
        return new AuroraServerlessCredentials(
                System.getenv("AWS_DB_ARN"),
                System.getenv("AWS_DB_SECRET_ARN")
        );
    }
}
