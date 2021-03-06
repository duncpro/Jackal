package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.StatementBuilderBase;
import com.duncpro.jackal.QueryResultRow;
import com.duncpro.jackal.StreamUtil;
import lombok.Value;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    private void closeJdbcConnection(Connection connection) {
        if (ownsConnection) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        }
    }

    private void closeJdbcStatement(PreparedStatement statement) {
        try {
            statement.close();
        } catch (SQLException e) {
            throw new AsyncSQLException(e);
        }
    }

    private void closeResultSet(ResultSet resultSet) {
        try {
            resultSet.close();
        } catch (SQLException e) {
            throw new AsyncSQLException(e);
        }
    }

    private CompletableFuture<PreparedStatement> compileJdbcStatement(Connection connection) {
        return supplyAsync(() -> {
            try {
                final var statement = connection.prepareStatement(parameterizedSQL, ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);

                for (int i = 0; i < args.size(); i++) {
                    final var arg = args.get(i);
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
        }, executor);
    }

    private CompletableFuture<ResultSetRowIterator.CountedResultSet> jdbcExecuteQuery(PreparedStatement statement) {
        final var resultSetFuture = supplyAsync(() -> {
            try {
                return statement.executeQuery();
            } catch (SQLException e) {
                throw new AsyncSQLException(e);
            }
        }, executor);

        return resultSetFuture.thenApplyAsync(resultSet -> {
            try {
                resultSet.last();
                final var length = resultSet.getRow();
                resultSet.beforeFirst();
                return new ResultSetRowIterator.CountedResultSet(resultSet, length);
            } catch (SQLException e) {
                final var wrappedException = new AsyncSQLException(e);
                try {
                    resultSet.close();
                } catch (SQLException e1) {
                   e.addSuppressed(e1);
                }
                throw wrappedException;
            }
        }, executor);
    }

    @Override
    public Stream<QueryResultRow> executeQueryImpl() {
        final var fullyAwareStream = connectionFuture.thenApply((connection) -> {

            final var statementAwareStreamFuture = compileJdbcStatement(connection).thenApply(statement -> {

                final var rsFuture = jdbcExecuteQuery(statement)
                        .thenApply(crs -> StreamSupport.stream(new ResultSetRowIterator(crs), false)
                                .onClose(() -> closeResultSet(crs.getResultSet())));

                return StreamUtil.unwrapStream(rsFuture)
                        .onClose(() -> closeJdbcStatement(statement));
            });

            // Connection-aware Stream
            return StreamUtil.unwrapStream(statementAwareStreamFuture)
                    .onClose(() -> closeJdbcConnection(connection));
        });

        return StreamUtil.unwrapStream(fullyAwareStream);

    }

    @Override
    protected CompletableFuture<Void> executeUpdateImpl() {
        return connectionFuture.thenCompose(connection ->
                compileJdbcStatement(connection)
                        .thenCompose(statement ->
                                jdbcExecuteUpdate(statement)
                                        .whenCompleteAsync(($, $$) -> closeJdbcStatement(statement), executor)
                        )
                        .whenCompleteAsync(($, $$) -> closeJdbcConnection(connection), executor)
        );
    }
}
