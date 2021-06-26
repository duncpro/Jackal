package com.duncpro.jackal;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;

@ThreadSafe
public interface QueryResultRow {
    String getString(String columnName);
    long getLong(String columnName);
    Boolean getBoolean(String columnName);

    public static QueryResultRow fromMap(Map<String, Object> map) {
        return new MapResultRow(map);
    }
}
