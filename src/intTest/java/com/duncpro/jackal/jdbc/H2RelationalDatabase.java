package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.RelationalDatabaseException;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.SQLException;
import java.util.concurrent.Executors;

public class H2RelationalDatabase extends DataSourceWrapper implements AutoCloseable {
    public H2RelationalDatabase() throws SQLException {
        super(new BasicDataSource(), Executors.newSingleThreadExecutor());
        ((BasicDataSource) dataSource).setDriverClassName("org.h2.Driver");
        ((BasicDataSource) dataSource).setUrl("jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        ((BasicDataSource) dataSource).start();
    }

    @Override
    public void close() throws RelationalDatabaseException, SQLException {
        prepareStatement("SHUTDOWN;").executeUpdate();
        executor.shutdownNow();
        ((BasicDataSource) dataSource).close();
    }
}
