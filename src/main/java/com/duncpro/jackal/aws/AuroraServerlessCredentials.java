package com.duncpro.jackal.aws;

import javax.annotation.concurrent.Immutable;

@Immutable
public class AuroraServerlessCredentials {
    final String dbArn;
    final String dbSecretArn;

    public AuroraServerlessCredentials(String dbArn, String dbSecretArn) {
        this.dbArn = dbArn;
        this.dbSecretArn = dbSecretArn;
    }
}
