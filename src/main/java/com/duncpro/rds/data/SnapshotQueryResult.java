package com.duncpro.rds.data;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class SnapshotQueryResult implements QueryResult {
    private final List<Map<String, Object>> snapshot;

    @Override
    public List<Map<String, Object>> toRowList() {
        final var protectedRows = new ArrayList<Map<String, Object>>();
        for (final var unprotectedRow : snapshot) {
            protectedRows.add(
                    Collections.unmodifiableMap(unprotectedRow)
            );
        }
        return Collections.unmodifiableList(protectedRows);
    }
}
