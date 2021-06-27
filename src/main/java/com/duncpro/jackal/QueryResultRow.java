package com.duncpro.jackal;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.Optional;

@ThreadSafe
public interface QueryResultRow {
    <T> Optional<T> get(String columnName, Class<T> javaType);

    static QueryResultRow fromMap(Map<String, Object> map) {
        return new MapResultRow(map);
    }
}
