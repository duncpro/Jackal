package com.duncpro.rds.data.jdbc;

import com.duncpro.rds.data.QueryResult;
import com.duncpro.rds.data.SnapshotQueryResult;
import com.duncpro.rds.data.StatementBuilderBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

class JdbcStatementBuilder extends StatementBuilderBase {
    private final CompletableFuture<Connection> connectionFuture;
    private final boolean ownsConnection;
    private final Executor executor;

    JdbcStatementBuilder(String parameterizedSQL, CompletableFuture<Connection> connectionFuture,
                                boolean ownsConnection, ExecutorService executor) {
        super(parameterizedSQL);
        this.connectionFuture = connectionFuture;
        this.ownsConnection = ownsConnection;
        this.executor = executor;
    }

    private CompletableFuture<Void> closeJdbcConnection(Connection connection) {
        return runAsync(() -> {
            if (ownsConnection) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new AsyncSQLException(e);
                }
            }
        }, executor);
    }


    private CompletableFuture<Void> closeJdbcStatement(PreparedStatement statement) {
        return runAsync(() -> {
            try {
                statement.close();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        }, executor);
    }

    private CompletableFuture<PreparedStatement> compileJdbcStatement(Connection connection) {
        return supplyAsync(() -> {
            try {
                final var statement = connection.prepareStatement(parameterizedSQL);

                for (int i = 0; i < args.length; i++) {
                    final var arg = args[i];
                    statement.setObject(i + 1, arg);
                }

                return statement;
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        }, executor);
    }


    private CompletableFuture<Void> jdbcExecuteUpdate(PreparedStatement statement) {
        return runAsync(() -> {
            try {
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        });
    }

    private QueryResult convertJdbcResult(ResultSet resultSet) throws SQLException {
        final var resultsSnapshot = new ArrayList<Map<String, Object>>();
        try (resultSet) {
            while (resultSet.next()) {
                final var rowSnapshot = new HashMap<String, Object>();
                for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
                    final var columnName = resultSet.getMetaData().getColumnName(i + 1);
                    final var value = resultSet.getObject(i + 1);
                    rowSnapshot.put(columnName, value);
                }
                resultsSnapshot.add(rowSnapshot);
            }
        }
        return new SnapshotQueryResult(resultsSnapshot);
    }

    private CompletableFuture<QueryResult> jdbcExecuteQuery(PreparedStatement statement) {
        return supplyAsync(() -> {
            try {
                final var resultSet = statement.executeQuery();
                return convertJdbcResult(resultSet);
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        });
    }

    @Override
    protected CompletableFuture<QueryResult> executeQueryImpl() {
        return connectionFuture.thenCompose(connection ->
                compileJdbcStatement(connection)
                        .thenCompose(statement ->
                                jdbcExecuteQuery(statement)
                                        .whenComplete(($, $$) -> closeJdbcStatement(statement))
                        )
                        .whenCompleteAsync(($, $$) -> closeJdbcConnection(connection).join())
        );
    }

    @Override
    protected CompletableFuture<Void> executeUpdateImpl() {
        return connectionFuture.thenCompose(connection ->
                compileJdbcStatement(connection)
                        .thenCompose(statement ->
                                jdbcExecuteUpdate(statement)
                                        .thenCompose(($) -> closeJdbcStatement(statement))
                        )
                        .whenCompleteAsync(($, $$) -> closeJdbcConnection(connection).join())
        );
    }
}
