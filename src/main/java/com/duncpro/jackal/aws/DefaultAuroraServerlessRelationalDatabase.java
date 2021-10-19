package com.duncpro.jackal.aws;

import com.duncpro.jackal.RelationalDatabaseException;
import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;

public class DefaultAuroraServerlessRelationalDatabase extends AuroraServerlessRelationalDatabase implements AutoCloseable {
    public DefaultAuroraServerlessRelationalDatabase(String dbArn, String dbSecretArn) {
        super(RdsDataClient.create(),  RdsDataAsyncClient.create(), dbArn, dbSecretArn);
    }

    @Override
    public void close() throws RelationalDatabaseException {
        super.rdsDataClient.close();
        super.rdsDataAsyncClient.close();
    }
}
