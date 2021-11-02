package com.duncpro.jackal.aws;

import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;

public class DefaultAuroraServerlessDatabase extends AuroraServerlessDatabase implements AutoCloseable {
    public DefaultAuroraServerlessDatabase(AuroraServerlessCredentials credentials) {
        super(new AuroraServerlessClientBundle(RdsDataClient.create(), RdsDataAsyncClient.create()), credentials);
    }

    @Override
    public void close() {
        super.clients.rdsDataClient.close();
        super.clients.rdsDataClient.close();
    }
}
