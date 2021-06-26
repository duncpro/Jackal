package com.duncpro.rds.data.jdbc;

import com.duncpro.rds.data.CreatePeopleTransaction;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DataSourceAsyncWrapperTest {
    private final BasicDataSource dataSource = new BasicDataSource();

    @Before
    public void startDatabase() throws SQLException {
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
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

    final DataSourceAsyncWrapper db = new DataSourceAsyncWrapper(dataSource, transactionExecutor, sqlExecutor);

    @Test
    public void testTransactions() {
        Set<String> expected = Set.of("David", "Helen", "Austin");

        db.commitTransactionAsync(new CreatePeopleTransaction(expected)).join();

        final var actual = db.prepareStatement("SELECT first_name FROM people;")
                .executeQuery()
                .map(row -> row.getString("first_name"))
                .collect(Collectors.toSet());

        Assert.assertEquals(expected, actual);
    }
}
