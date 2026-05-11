package com.JobsNow.backend.util;

public final class PagingUtil {

    private PagingUtil() {
    }

    public static int safePage(int page) {
        return Math.max(page, 1);
    }

    public static int safeLimit(int limit, int max) {
        return Math.max(1, Math.min(limit, max));
    }
}
