package com.sandeep.urlshortener.cache;

/**
 * Centralized cache names.
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    /**
     * Cache for URL lookups.
     */
    public static final String URL_CACHE = "urlCache";

    /**
     * Cache for URL statistics.
     */
    public static final String URL_STATS_CACHE = "urlStatsCache";
}