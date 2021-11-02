package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

public class JDBCSQLExecutor extends BlockingSQLExecutor {
    private final ConnectionSupplier connection;
    private final boolean ownsConnection;

    JDBCSQLExecutor(final ExecutorService taskExecutor, final ConnectionSupplier connection, final boolean ownsConnection) {
        super(taskExecutor);
        this.connection = connection;
        this.ownsConnection = ownsConnection;
    }

    private static QueryResultRow copyCurrentRow(ResultSet resultSet) throws java.sql.SQLException {
        final var rowSnapshot = new HashMap<String, Object>();
        for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
            final var columnName = resultSet.getMetaData().getColumnName(i + 1);
            final var value = resultSet.getObject(i + 1);
            rowSnapshot.put(columnName, value);
        }
        return new MapResultRow(rowSnapshot);
    }

    private static void applyArgs(List<Object> args, PreparedStatement statement) throws java.sql.SQLException {
        for (int i = 0; i < args.size(); i++) {
            final var arg = args.get(i);
            statement.setObject(i + 1, arg);
        }
    }

    // Execute Query

    private Stream<QueryResultRow> executeQuery(InterpolatedSQLStatement sql, Connection connection) throws SQLException {
        try (final var statement = connection.prepareStatement(sql.getParameterizedScript())) {
            applyArgs(sql.getArgs(), statement);
            final var results = new ArrayList<QueryResultRow>();
            try (final var resultSet = statement.executeQuery(); statement) {
                while (resultSet.next()) {
                    results.add(copyCurrentRow(resultSet));
                }
            }
            return results.stream();
        } catch (java.sql.SQLException e) {
            throw new SQLException(e);
        }
    }

    public Stream<QueryResultRow> executeQueryAndCloseConnection(InterpolatedSQLStatement sql) throws SQLException {
        try (final var connection = this.connection.get()) {
            return executeQuery(sql, connection);
        } catch (java.sql.SQLException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Stream<QueryResultRow> executeQuery(InterpolatedSQLStatement sql) throws SQLException {
        if (this.ownsConnection) {
            return executeQueryAndCloseConnection(sql);
        } else {
            try {
                return executeQuery(sql, this.connection.get());
            } catch (java.sql.SQLException e) {
                throw new SQLException(e);
            }
        }
    }

    // Execute Update

    public void executeUpdateAndCloseConnection(InterpolatedSQLStatement sql) throws SQLException {
        try (final var connection = this.connection.get()) {
            executeUpdate(sql, connection);
        } catch (java.sql.SQLException e) {
            throw new SQLException(e);
        }
    }

    private void executeUpdate(InterpolatedSQLStatement sql, Connection connection) throws SQLException {
        try (final var statement = connection.prepareStatement(sql.getParameterizedScript())) {
            applyArgs(sql.getArgs(), statement);
            statement.executeUpdate();
        } catch (java.sql.SQLException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void executeUpdate(InterpolatedSQLStatement sql) throws SQLException {
        if (this.ownsConnection) {
            executeUpdateAndCloseConnection(sql);
        } else {
            try {
                executeUpdate(sql, connection.get());
            } catch (java.sql.SQLException e) {
                throw new SQLException(e);
            }
        }
    }
}
