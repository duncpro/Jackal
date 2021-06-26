package com.duncpro.rds.data;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class MapResultRow implements QueryResultRow {
    private final Map<String, Object> map;

    @Override
    public String getString(String columnName) {
        return (String) map.get(columnName);
    }

    @Override
    public long getLong(String columnName) {
        return (long) map.get(columnName);
    }

    @Override
    public Boolean getBoolean(String columnName) {
        return (Boolean) map.get(columnName);
    }
}
