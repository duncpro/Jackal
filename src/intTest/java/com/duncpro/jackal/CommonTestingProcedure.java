package com.duncpro.jackal;

public interface CommonTestingProcedure {
    void setup(RelationalDatabase db);

    void test(RelationalDatabase db);

    default void apply(RelationalDatabase db) {
        setup(db);
        test(db);
    }
}
