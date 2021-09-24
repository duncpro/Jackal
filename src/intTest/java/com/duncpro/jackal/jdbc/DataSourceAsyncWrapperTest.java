package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.*;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.*;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    final DataSourceWrapper db = new DataSourceWrapper(dataSource, transactionExecutor, sqlExecutor);

    @Test
    public void testCommitTransactionAsync() {
        new CommitTransactionAsyncTestProcedure().accept(db);
    }

    @Test
    public void testRollback() {
        new RollbackTestProcedure().accept(db);
    }

    @Test
    public void testImplicitRollback() {
        new ImplicitRollbackTestProcedure().accept(db);
    }

    @Test
    public void testTypeConversions() { new TypeTestingProcedure().apply(db); }

    @Test
    @Ignore
    public void parallelizationBenchmark() {
        new ParallelizationBenchmark().accept(db);
    }
}
