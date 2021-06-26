package com.duncpro.rds.data.jdbc;

import com.duncpro.rds.data.QueryResultRow;
import lombok.RequiredArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Spliterator;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ResultSetRowIterator implements Spliterator<QueryResultRow> {
    private final ResultSet resultSet;

    // The row the ResultSet is currently on. Zero if next() has never been called.
    private int currentRow = 0;

    @Override
    public boolean tryAdvance(Consumer<? super QueryResultRow> action) {
        try {
            final var isRemaining = resultSet.next();
            if (isRemaining) {
                action.accept(copyCurrentRow());
                currentRow++;
            }
            return isRemaining;
        } catch (SQLException e) {
            throw new AsyncSQLException(e);
        }
    }

    @Override
    public Spliterator<QueryResultRow> trySplit() {
        // ResultSet cannot be split without making another request to the database
        return null;
    }

    @Override
    public long estimateSize() {
        return getResultSetFullLength() - currentRow;
    }

    @Override
    public int characteristics() {
        return Spliterator.ORDERED;
    }

    int getResultSetFullLength() {
        try {
            resultSet.last();
            return resultSet.getRow();
        } catch (SQLException e) {
            throw new AsyncSQLException(e);
        }
    }

    private QueryResultRow copyCurrentRow() throws SQLException {
        final var rowSnapshot = new HashMap<String, Object>();
        for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
            final var columnName = resultSet.getMetaData().getColumnName(i + 1);
            final var value = resultSet.getObject(i + 1);
            rowSnapshot.put(columnName, value);
        }
        return QueryResultRow.fromMap(rowSnapshot);
    }
}
