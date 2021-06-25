package com.duncpro.rds.data.jdbc;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class DataSourceAsyncDatabaseTest {
    private final BasicDataSource dataSource = new BasicDataSource();

    @Before
    public void startDatabase() throws SQLException {
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:test");
        System.out.println(dataSource.getUrl());
        dataSource.start();
    }

    @After
    public void shutdownDatabase() throws SQLException {
        dataSource.close();
    }

    private final ExecutorService transactionExecutor = Executors.newCachedThreadPool();
    private final ExecutorService sqlExecutor = Executors.newCachedThreadPool();

    @After
    public void shutdownExecutor() {
        transactionExecutor.shutdown();
        sqlExecutor.shutdown();
    }

    final DataSourceAsyncDatabase asyncDb = new DataSourceAsyncDatabase(dataSource, transactionExecutor, sqlExecutor);

    @Test
    public void selectRecordTransaction() {
        final var retrievedValue = asyncDb.commitTransactionAsync(transaction -> {
            transaction.prepareStatement("CREATE TABLE TABLE_A (COLUMN_A varchar);")
                    .executeUpdate()
                    .join();

            asyncDb.prepareStatement("INSERT INTO TABLE_A VALUES (?);")
                    .setString(0, "hello")
                    .executeUpdate()
                    .join();

            return asyncDb.prepareStatement("SELECT * FROM TABLE_A;")
                    .executeQuery()
                    .join()
                    .toRowList().get(0).get("COLUMN_A");
        });

        assertEquals("hello", retrievedValue.join());
    }
}
