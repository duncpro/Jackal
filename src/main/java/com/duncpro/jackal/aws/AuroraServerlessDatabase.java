package com.duncpro.jackal.aws;

import com.duncpro.jackal.SQLDatabase;
import com.duncpro.jackal.SQLException;
import com.duncpro.jackal.SQLExecutor;
import com.duncpro.jackal.SQLTransaction;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rdsdata.model.BeginTransactionRequest;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class AuroraServerlessDatabase extends SQLDatabase {
    protected final AuroraServerlessClientBundle clients;
    private final AuroraServerlessCredentials credentials;

    public AuroraServerlessDatabase(final AuroraServerlessClientBundle clients,
                                    final AuroraServerlessCredentials credentials) {
        this.clients = clients;
        this.credentials = credentials;
    }

    @Override
    public SQLTransaction startTransaction() throws SQLException {
        final var request = BeginTransactionRequest.builder()
                .resourceArn(this.credentials.dbArn)
                .secretArn(this.credentials.dbSecretArn)
                .build();

        try {
            final var transactionId = this.clients.rdsDataClient.beginTransaction(request).transactionId();
            return new AuroraServerlessTransaction(new AuroraServerlessSQLExecutor(transactionId, this.clients,
                    this.credentials));
        } catch (SdkException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public SQLExecutor getExecutor() {
        return new AuroraServerlessSQLExecutor(null, this.clients, this.credentials);
    }
}
