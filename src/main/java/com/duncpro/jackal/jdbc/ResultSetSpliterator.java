package com.duncpro.jackal.jdbc;

import com.duncpro.jackal.QueryResultRow;
import com.duncpro.jackal.UncheckedSQLException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Spliterator;
import java.util.function.Consumer;

import static com.duncpro.jackal.jdbc.JDBCSQLExecutor.copyCurrentRow;
import static java.util.Objects.requireNonNull;

public class ResultSetSpliterator implements Spliterator<QueryResultRow> {
    static final int CHARACTERISTICS = Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.NONNULL;

    private final ResultSet resultSet;

    ResultSetSpliterator(ResultSet resultSet) {
        this.resultSet = requireNonNull(resultSet);
    }

    @Override
    public boolean tryAdvance(Consumer<? super QueryResultRow> action) {
        try {
            if (!resultSet.next()) return false;
            action.accept(copyCurrentRow(resultSet));
            return true;
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    @Override
    public Spliterator<QueryResultRow> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return CHARACTERISTICS;
    }
}
