package com.duncpro.jackal;

import org.intellij.lang.annotations.Language;

import javax.annotation.concurrent.Immutable;
import java.nio.channels.AsynchronousFileChannel;
import java.util.List;

@Immutable
public class InterpolatedSQLStatement {
    private final InterpolatableSQLStatement wrapped;

    InterpolatedSQLStatement(final InterpolatableSQLStatement wrapped) {
        if (wrapped.sql.countParameters() != wrapped.countArguments())
            throw new IllegalStateException();

        this.wrapped = wrapped;
    }

    public List<Object> getArgs() { return wrapped.args; }

    @Language("SQL")
    public String getParameterizedScript() { return wrapped.sql.toString(); }
}
