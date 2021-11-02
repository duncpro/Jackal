package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.SQLDatabase;
import com.duncpro.jackal.SQLException;
import com.duncpro.jackal.SQLExecutor;
import com.duncpro.jackal.SQLTransaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public class DataSourceWrapper extends SQLDatabase {
    final ExecutorService taskExecutor;
    final DataSource dataSource;

    public DataSourceWrapper(final ExecutorService taskExecutor, final DataSource dataSource) {
        this.taskExecutor = taskExecutor;
        this.dataSource = dataSource;
    }

    @Override
    public SQLTransaction startTransaction() throws SQLException {
        final Connection connection;
        try {
            connection = dataSource.getConnection();
        }  catch (java.sql.SQLException e) {
            throw new SQLException(e);
        }

        try {
            connection.setAutoCommit(false);
        } catch (java.sql.SQLException e) {
            try {
                connection.close();
            } catch (java.sql.SQLException e1) {
                e.addSuppressed(e1);
            }
            throw new SQLException(e);
        }

        return new JDBCTransaction(taskExecutor, connection);
    }

    private Connection getAutoCommitConnection() throws java.sql.SQLException {
        final var con = dataSource.getConnection();
        try {
            con.setAutoCommit(true);
        } catch (java.sql.SQLException e) {
            try {
                con.close();
            } catch (java.sql.SQLException e1) {
                e.addSuppressed(e1);
            }
            throw e;
        }
        return con;
    }

    @Override
    protected SQLExecutor getExecutor() {
        return new JDBCSQLExecutor(taskExecutor, this::getAutoCommitConnection, true);
    }
}
