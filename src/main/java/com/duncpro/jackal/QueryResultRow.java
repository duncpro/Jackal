package com.duncpro.jackal;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.Optional;

@ThreadSafe
public interface QueryResultRow {
    /**
     * Returns an optional representing the value stored in the given column of this row.
     * This function will throw an {@link IllegalArgumentException} if the given column name does
     * not exist within the row.
     */
    <T> Optional<T> get(String columnName, Class<T> javaType);

    static QueryResultRow fromMap(Map<String, Object> map) {
        return new MapResultRow(map);
    }
}
