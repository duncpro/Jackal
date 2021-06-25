package com.duncpro.rds.data.impl;

import com.duncpro.rds.data.QueryResult;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementResponse;

import java.util.*;

@RequiredArgsConstructor
class AmazonRDSQueryResult implements QueryResult {
    private final ExecuteStatementResponse awsResponse;

    @Override
    public List<Map<String, Object>> toRowList() {
        final var rowList = new ArrayList<Map<String, Object>>();
        for (final var awsRowRecord : awsResponse.records()) {
            final var row = new HashMap<String, Object>();

            for (int columnIndex = 0; columnIndex < awsResponse.columnMetadata().size(); columnIndex++) {
                final var columnName = awsResponse.columnMetadata().get(columnIndex).name();
                final var typeName = awsResponse.columnMetadata().get(columnIndex).typeName();
                final var serializedValue = awsRowRecord.get(columnIndex);

                Object deserializedValue;

                if (typeName.equals("varchar")) {
                    deserializedValue = serializedValue.stringValue();
                } else if (typeName.equals("bool")) {
                    deserializedValue = serializedValue.booleanValue();
                } else if (typeName.contains("int")) {
                    deserializedValue = serializedValue.longValue();
                } else {
                    throw new AssertionError("Unexpected data type: " + typeName);
                }

                row.put(columnName, deserializedValue);
            }

            rowList.add(
                    Collections.unmodifiableMap(row)
            );
        }

        return Collections.unmodifiableList(rowList);
    }
}
