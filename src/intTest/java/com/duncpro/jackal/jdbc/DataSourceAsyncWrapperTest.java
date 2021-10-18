package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.*;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.*;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataSourceAsyncWrapperTest {
    private static final BasicDataSource dataSource = new BasicDataSource();

    @BeforeClass
    public static void startDatabase() throws SQLException {
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        dataSource.start();
    }

    @AfterClass
    public static void shutdownDatabase() throws SQLException {
        dataSource.close();
    }

    private static final ExecutorService sqlExecutor = Executors.newCachedThreadPool();

    @AfterClass
    public static void shutdownExecutor() {
        sqlExecutor.shutdown();
    }

    final DataSourceWrapper db = new DataSourceWrapper(dataSource, sqlExecutor);

    @Test
    public void testTypeConversions() { new TypeTestingProcedure().apply(db); }

    @Test
    @Ignore
    public void parallelizationBenchmark() {
        new ParallelizationBenchmark().apply(db);
    }

    @Test
    public void testImplicitRollback() {
        new ImplicitRollbackTestingProcedure().apply(db);
    }

    @Test
    public void testCommitTransaction() {
        new CommitTransactionTestingProcedure().apply(db);
    }
}
