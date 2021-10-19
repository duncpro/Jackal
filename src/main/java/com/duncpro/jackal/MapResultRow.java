package com.duncpro.jackal;

import java.util.Map;
import java.util.Optional;

public class MapResultRow implements QueryResultRow {
    private final Map<String, Object> columnValueMap;

    public MapResultRow(Map<String, Object> columnValueMap) {
        this.columnValueMap = Map.copyOf(columnValueMap);
    }

    @Override
    public <T> Optional<T> get(String columnName, Class<T> javaType) {
        final var rawValue = columnValueMap.get(columnName);
        if (rawValue == null) return Optional.empty();
        final T casted;
        try {
            casted = javaType.cast(rawValue);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The requested data type: \"" + javaType.getSimpleName() + "\" does not" +
                    " match the data type returned by the SQL client implementation.", e);
        }
        return Optional.of(casted);
    }
}
