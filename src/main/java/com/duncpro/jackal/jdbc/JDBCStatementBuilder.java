package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.SQLStatementBuilderBase;
import com.duncpro.jackal.QueryResultRow;
import com.duncpro.jackal.RelationalDatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class JDBCStatementBuilder extends SQLStatementBuilderBase {
    private final ConnectionSupplier connectionSupplier;
    private final boolean ownsConnection;
    private final ExecutorService executor;

    JDBCStatementBuilder(ConnectionSupplier connectionSupplier, boolean ownsConnection, ExecutorService executor, String parameterizedSQL) {
        super(parameterizedSQL);
        this.connectionSupplier = connectionSupplier;
        this.ownsConnection = ownsConnection;
        this.executor = executor;
    }

    private static QueryResultRow copyCurrentRow(ResultSet resultSet) throws SQLException {
        final var rowSnapshot = new HashMap<String, Object>();
        for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
            final var columnName = resultSet.getMetaData().getColumnName(i + 1);
            final var value = resultSet.getObject(i + 1);
            rowSnapshot.put(columnName, value);
        }
        return QueryResultRow.fromMap(rowSnapshot);
    }

    private void applyArgs(PreparedStatement statement) throws SQLException {
        for (int i = 0; i < args.size(); i++) {
            final var arg = args.get(i);
            statement.setObject(i + 1, arg);
        }
    }

    public Stream<QueryResultRow> executeQueryAndCloseConnection() throws RelationalDatabaseException {
        try (final var connection = this.connectionSupplier.get()) {
            return executeQuery(connection);
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public Stream<QueryResultRow> executeQuery() throws RelationalDatabaseException {
        if (this.ownsConnection) {
            return executeQueryAndCloseConnection();
        } else {
            try {
                return executeQuery(connectionSupplier.get());
            } catch (SQLException e) {
                throw new RelationalDatabaseException(e);
            }
        }
    }

    private Stream<QueryResultRow> executeQuery(Connection connection) throws RelationalDatabaseException {
        try (final var statement = connection.prepareStatement(parameterizedSQL)) {
            applyArgs(statement);
            final var results = new ArrayList<QueryResultRow>();
            try (final var resultSet = statement.executeQuery(); statement) {
                while (resultSet.next()) {
                    results.add(copyCurrentRow(resultSet));
                }
            }
            return results.stream();
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public CompletableFuture<Stream<QueryResultRow>> executeQueryAsync() {
        final var future = new CompletableFuture<Stream<QueryResultRow>>();
        executor.submit(() -> {
            try {
                future.complete(executeQuery());
            } catch (RelationalDatabaseException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void executeUpdate(Connection connection) throws RelationalDatabaseException {
        try (final var statement = connection.prepareStatement(parameterizedSQL)) {
            applyArgs(statement);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    public void executeUpdateAndCloseConnection() throws RelationalDatabaseException {
        try (final var connection = this.connectionSupplier.get()) {
            executeUpdate(connection);
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public void executeUpdate() throws RelationalDatabaseException {
        if (this.ownsConnection) {
            executeUpdateAndCloseConnection();
        } else {
            try {
                executeUpdate(connectionSupplier.get());
            } catch (SQLException e) {
                throw new RelationalDatabaseException(e);
            }
        }
    }

    @Override
    public CompletableFuture<Void> executeUpdateAsync() {
        final var future = new CompletableFuture<Void>();
        executor.submit(() -> {
            try {
                executeUpdate();
                future.complete(null);
            } catch (RelationalDatabaseException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
