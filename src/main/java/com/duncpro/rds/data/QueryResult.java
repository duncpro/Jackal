package com.duncpro.rds.data;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Map;

@NotThreadSafe
public interface QueryResult {
    /**
     * Returns the complete result set as a list of rows. Each element in the list is map which links
     * the column name to the column value.
     *
     * Attempts to modify the returned collections will result in {@link UnsupportedOperationException}.
     *
     * @deprecated This function is deprecated because it returns values with cloudy types (Object).
     * It will be removed in a later release once a suitable replacement is developed.
     */
    @Deprecated(forRemoval = true)
    List<Map<String, Object>> toRowList();
}
