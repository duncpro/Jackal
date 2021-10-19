package com.duncpro.jackal;

import org.intellij.lang.annotations.Language;

public interface SQLStatementBuilderFactory {
    SQLStatementBuilder prepareStatement(@Language("SQL") String parameterizedSQL);
}
