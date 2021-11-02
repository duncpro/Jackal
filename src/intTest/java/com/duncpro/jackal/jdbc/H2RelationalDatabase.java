package com.duncpro.jackal.jdbc;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.SQLException;
import java.util.concurrent.Executors;

import static com.duncpro.jackal.InterpolatableSQLStatement.sql;

public class H2RelationalDatabase extends DataSourceWrapper implements AutoCloseable {
    public H2RelationalDatabase() throws SQLException {
        super(Executors.newSingleThreadExecutor(), new BasicDataSource());
        ((BasicDataSource) dataSource).setDriverClassName("org.h2.Driver");
        ((BasicDataSource) dataSource).setUrl("jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ((BasicDataSource) dataSource).start();
    }

    @Override
    public void close() throws SQLException, com.duncpro.jackal.SQLException {
        sql("SHUTDOWN;").executeUpdate(this);
        super.taskExecutor.shutdownNow();
        ((BasicDataSource) dataSource).close();
    }
}
