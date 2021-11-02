package com.duncpro.jackal.aws;

import software.amazon.awssdk.services.rdsdata.RdsDataAsyncClient;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;

public class AuroraServerlessClientBundle {
    final RdsDataClient rdsDataClient;
    final RdsDataAsyncClient rdsDataAsyncClient;

    public AuroraServerlessClientBundle(RdsDataClient rdsDataClient, RdsDataAsyncClient rdsDataAsyncClient) {
        this.rdsDataClient = rdsDataClient;
        this.rdsDataAsyncClient = rdsDataAsyncClient;
    }
}
