package com.duncpro.jackal.jdbc;



import com.duncpro.jackal.RelationalDatabase;
import com.duncpro.jackal.RelationalDatabaseException;
import com.duncpro.jackal.RelationalDatabaseTransactionHandle;
import com.duncpro.jackal.SQLStatementBuilder;
import org.intellij.lang.annotations.Language;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

public class DataSourceWrapper implements RelationalDatabase {
    protected final DataSource dataSource;
    protected final ExecutorService executor;

    public DataSourceWrapper(DataSource dataSource, ExecutorService executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }


    @Override
    public RelationalDatabaseTransactionHandle startTransaction() throws RelationalDatabaseException {
        try {
            return new JDBCTransactionHandle(executor, dataSource.getConnection());
        } catch (SQLException e) {
            throw new RelationalDatabaseException(e);
        }
    }

    @Override
    public SQLStatementBuilder prepareStatement(String parameterizedSQL) {
        return new JDBCStatementBuilder(dataSource::getConnection, true, executor, parameterizedSQL);
    }
}
