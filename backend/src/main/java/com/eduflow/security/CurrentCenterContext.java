package com.eduflow.security;

public final class CurrentCenterContext {
    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();

    private CurrentCenterContext() {
    }

    public static void set(Long centerId) {
        CONTEXT.set(centerId);
    }

    public static Long get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
