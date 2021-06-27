package com.duncpro.jackal;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class MapResultRow implements QueryResultRow {
    private final Map<String, Object> columnValueMap;
    @Override
    public <T> Optional<T> get(String columnName, Class<T> javaType) {
        final var rawValue = columnValueMap.get(columnName);

        if (rawValue == null) return Optional.empty();

        return Optional.of(javaType.cast(rawValue));
    }
}
