package com.duncpro.jackal.rds;

import com.duncpro.jackal.QueryResultRow;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.rdsdata.model.Field;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class AmazonDataAPIRow implements QueryResultRow {
    private final Map<String, Field> awsRow;

    @Override
    public <T> Optional<T> get(String columnName, Class<T> javaType) {
        if (!awsRow.containsKey(columnName))
            throw new IllegalArgumentException("No column by the name " + columnName + " exists within this row.");

        final var valueWrapper = awsRow.get(columnName);

        if (valueWrapper.isNull() != null && valueWrapper.isNull()) return Optional.empty();

        final T unwrappedValue;

        if (javaType == String.class) {
            unwrappedValue = (T) valueWrapper.stringValue();
        } else if (javaType == Long.class || javaType == long.class) {
            unwrappedValue = (T) valueWrapper.longValue();
        } else if (javaType == Integer.class || javaType == int.class) {
            unwrappedValue = (T) Integer.valueOf(valueWrapper.longValue().intValue());
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            unwrappedValue = (T) valueWrapper.booleanValue();
        } else if (javaType == byte[].class) {
            unwrappedValue = (T) valueWrapper.blobValue().asByteArray();
        } else if (javaType == Double.class || javaType == double.class) {
            unwrappedValue = (T) valueWrapper.doubleValue();
        } else if (javaType == BigDecimal.class) {
            unwrappedValue = (T) new BigDecimal(valueWrapper.stringValue());
        }
        else {
            throw new IllegalArgumentException("Deserializing " + javaType.getName() + " is unsupported.");
        }

        return Optional.of(unwrappedValue);
    }
}
