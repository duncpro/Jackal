package com.duncpro.jackal;

import java.util.Map;

public interface QueryResultRow {
    String getString(String columnName);
    long getLong(String columnName);
    Boolean getBoolean(String columnName);

    public static QueryResultRow fromMap(Map<String, Object> map) {
        return new MapResultRow(map);
    }
}
