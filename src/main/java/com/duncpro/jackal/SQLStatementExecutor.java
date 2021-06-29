package com.duncpro.jackal;

import org.intellij.lang.annotations.Language;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
interface SQLStatementExecutor {
    StatementBuilder prepareStatement(@Language("SQL") String parameterizedSQL);
}
