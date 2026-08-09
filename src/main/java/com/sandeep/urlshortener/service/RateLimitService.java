package com.sandeep.urlshortener.service;

public interface RateLimitService {

    /**
     * Checks whether the request is allowed for the given key.
     *
     * @param key Redis key
     * @param limit Maximum requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if request is allowed, false otherwise
     */
    boolean isAllowed(String key, int limit, long windowSeconds);

    /**
     * Returns the remaining requests in the current window.
     *
     * @param key Redis key
     * @param limit Maximum allowed requests
     * @return remaining requests
     */
    long getRemainingRequests(String key, int limit);

    /**
     * Returns the number of seconds until the rate-limit window resets.
     *
     * @param key Redis key
     * @return remaining TTL in seconds
     */
    long getRetryAfterSeconds(String key);
}