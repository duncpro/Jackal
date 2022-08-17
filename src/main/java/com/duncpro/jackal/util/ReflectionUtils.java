package com.duncpro.jackal.util;

import static java.util.Objects.requireNonNull;

public class ReflectionUtils {
    public static Class<?> box(Class<?> target) {
        if (!target.isPrimitive()) return target;
        if (target.equals(int.class)) return Integer.class;
        if (target.equals(long.class)) return Long.class;
        if (target.equals(short.class)) return short.class;
        if (target.equals(double.class)) return Double.class;
        if (target.equals(float.class)) return Float.class;
        if (target.equals(boolean.class)) return Boolean.class;
        if (target.equals(char.class)) return Character.class;
        if (target.equals(byte.class)) return Byte.class;
        throw new AssertionError();
    }
}
