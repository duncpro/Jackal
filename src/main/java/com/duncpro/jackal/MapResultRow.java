package com.duncpro.jackal;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class MapResultRow implements QueryResultRow {
    private final Map<String, Object> columnValueMap;
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
