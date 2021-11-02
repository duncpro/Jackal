package com.duncpro.jackal;

import org.intellij.lang.annotations.Language;

import javax.annotation.concurrent.Immutable;
import java.util.regex.Pattern;

@Immutable
public final class ParameterizableSQLStatement {
    private final String sqlScript;

    ParameterizableSQLStatement(@Language("SQL") final String sqlScript) {
        this.sqlScript = sqlScript;
    }

    /**
     * Calculates the number of parameters which exist within the SQL script.
     */
    final int countParameters() {
        return sqlScript.length() - (sqlScript.replaceAll(Pattern.quote("?"), "").length());
    }

    /**
     * Returns the raw parameterized sql script which this object encapsulates.
     */
    @Override
    public String toString() {
        return sqlScript;
    }
}
