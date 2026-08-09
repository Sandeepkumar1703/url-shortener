package com.sandeep.urlshortener.service.impl;

import com.sandeep.urlshortener.cache.CacheConstants;
import com.sandeep.urlshortener.dto.request.CreateShortUrlRequest;
import com.sandeep.urlshortener.dto.response.CreateShortUrlResponse;
import com.sandeep.urlshortener.dto.response.UrlStatsResponse;
import com.sandeep.urlshortener.entity.Url;
import com.sandeep.urlshortener.exception.ResourceNotFoundException;
import com.sandeep.urlshortener.exception.UrlExpiredException;
import com.sandeep.urlshortener.repository.UrlRepository;
import com.sandeep.urlshortener.service.UrlService;
import com.sandeep.urlshortener.util.ShortCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ========================================================================
 * URL Service Implementation
 * ========================================================================
 *
 * Provides business logic for:
 *
 * 1. Creating shortened URLs
 * 2. Resolving short codes
 * 3. Tracking click counts
 * 4. Retrieving URL statistics
 * 5. Deleting shortened URLs
 * 6. Redis caching
 *
 * Cache strategy:
 *
 * Client
 *   |
 *   v
 * UrlServiceImpl
 *   |
 *   v
 * Redis
 *   |
 *   +---- HIT ----> URL
 *   |
 *   +---- MISS ---> PostgreSQL ---> Redis ---> URL
 *
 * PostgreSQL remains the source of truth.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    /**
     * PostgreSQL repository.
     */
    private final UrlRepository repository;

    /**
     * Generates unique short codes.
     */
    private final ShortCodeGenerator generator;

    /**
     * Spring CacheManager backed by Redis.
     */
    private final CacheManager cacheManager;


    /**
     * ========================================================================
     * Create Short URL
     * ========================================================================
     */
    @Override
    public CreateShortUrlResponse createShortUrl(
            CreateShortUrlRequest request) {

        log.info(
                "Creating short URL for original URL: {}",
                request.getOriginalUrl()
        );

        /*
         * Generate a unique short code.
         */
        String code;

        do {
            code = generator.generate();
        } while (repository.existsByShortCode(code));

        /*
         * Build URL entity.
         */
        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(code)
                .expiresAt(request.getExpiresAt())
                .build();

        /*
         * Save URL in PostgreSQL.
         */
        Url savedUrl = repository.save(url);

        log.info(
                "Short URL created successfully. Code={}",
                savedUrl.getShortCode()
        );

        /*
         * Cache the newly created URL.
         */
        putInCache(savedUrl);

        /*
         * Build response.
         */
        return CreateShortUrlResponse.builder()
                .shortCode(savedUrl.getShortCode())
                .originalUrl(savedUrl.getOriginalUrl())
                .shortUrl(
                        "http://localhost:8080/"
                                + savedUrl.getShortCode()
                )
                .createdAt(savedUrl.getCreatedAt())
                .expiresAt(savedUrl.getExpiresAt())
                .clickCount(savedUrl.getClickCount())
                .build();
    }


    /**
     * ========================================================================
     * Resolve Short URL
     * ========================================================================
     *
     * IMPORTANT:
     *
     * We intentionally do NOT use @Cacheable here.
     *
     * Why?
     *
     * Because every successful redirect must increment clickCount.
     *
     * @Cacheable would return the cached value and skip the method body
     * on subsequent requests, which would prevent clickCount from being
     * updated correctly.
     */
    @Override
    public String getOriginalUrl(String shortCode) {

        log.info(
                "Resolving short URL. Code={}",
                shortCode
        );

        /*
         * ------------------------------------------------------------
         * STEP 1: Check Redis
         * ------------------------------------------------------------
         */
        Url url = getFromCache(shortCode);

        if (url != null) {

            log.info(
                    "Redis cache HIT. Code={}",
                    shortCode
            );

            /*
             * Check expiration even when the URL came from Redis.
             */
            if (isExpired(url)) {

                log.info(
                        "Cached URL has expired. Code={}",
                        shortCode
                );

                evictFromCache(shortCode);

                throw new UrlExpiredException(
                        "Short URL has expired: " + shortCode
                );
            }

            /*
             * Increment click count.
             * (incrementClickCount() already persists to PostgreSQL
             * and refreshes Redis via putInCache() internally.)
             */
            incrementClickCount(url);

            return url.getOriginalUrl();
        }


        /*
         * ------------------------------------------------------------
         * STEP 2: Redis MISS
         * ------------------------------------------------------------
         */
        log.info(
                "Redis cache MISS. Code={}",
                shortCode
        );

        /*
         * Retrieve URL from PostgreSQL.
         */
        url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> {

                    log.warn(
                            "Short URL not found. Code={}",
                            shortCode
                    );

                    return new ResourceNotFoundException(
                            "Short URL not found: " + shortCode
                    );
                });


        /*
         * ------------------------------------------------------------
         * STEP 3: Check expiration
         * ------------------------------------------------------------
         */
        if (isExpired(url)) {

            log.info(
                    "URL has expired. Code={}",
                    shortCode
            );

            /*
             * Remove expired URL from Redis if it exists.
             */
            evictFromCache(shortCode);

            throw new UrlExpiredException(
                    "Short URL has expired: " + shortCode
            );
        }


        /*
         * ------------------------------------------------------------
         * STEP 4: Increment click count
         * ------------------------------------------------------------
         *
         * incrementClickCount() persists the updated click count to
         * PostgreSQL AND refreshes Redis (single putInCache call).
         * We do NOT call putInCache() again after this — doing so
         * would write to Redis twice for a single resolution.
         */
        incrementClickCount(url);


        /*
         * ------------------------------------------------------------
         * STEP 5: Return original URL
         * ------------------------------------------------------------
         */
        return url.getOriginalUrl();
    }


    /**
     * ========================================================================
     * Increment Click Count
     * ========================================================================
     */
    private void incrementClickCount(Url url) {

        long currentCount = url.getClickCount() == null
                ? 0L
                : url.getClickCount();

        url.setClickCount(currentCount + 1L);

        /*
        * Persist updated click count in PostgreSQL.
        */
        Url updatedUrl = repository.save(url);

        /*
        * Update Redis with the latest entity.
        */
        putInCache(updatedUrl);

        log.info(
                "Click count incremented. Code={}, Clicks={}",
                updatedUrl.getShortCode(),
                updatedUrl.getClickCount()
        );
        }


    /**
     * ========================================================================
     * Check URL Expiration
     * ========================================================================
     */
    private boolean isExpired(Url url) {

        return url.getExpiresAt() != null
                && url.getExpiresAt().isBefore(LocalDateTime.now());
    }


    /**
     * ========================================================================
     * Get URL Statistics
     * ========================================================================
     *
     * Statistics are always read from PostgreSQL because PostgreSQL
     * is the source of truth.
     */
    @Override
    public UrlStatsResponse getStatistics(String shortCode) {

        log.info(
                "Fetching statistics for short code: {}",
                shortCode
        );

        Url url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> {

                    log.warn(
                            "Statistics requested for unknown short code: {}",
                            shortCode
                    );

                    return new ResourceNotFoundException(
                            "Short URL '" + shortCode + "' not found"
                    );
                });

        return UrlStatsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .build();
    }


    /**
     * ========================================================================
     * Delete Short URL
     * ========================================================================
     */
    @Override
    public void deleteShortUrl(String shortCode) {

        log.info(
                "Deleting short URL: {}",
                shortCode
        );

        /*
         * Find URL first so that we can return 404 when it doesn't exist.
         */
        Url url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> {

                    log.warn(
                            "Delete failed. Short URL not found: {}",
                            shortCode
                    );

                    return new ResourceNotFoundException(
                            "Short URL '" + shortCode + "' not found"
                    );
                });

        /*
         * Delete from PostgreSQL.
         */
        repository.delete(url);

        /*
         * Delete from Redis.
         */
        evictFromCache(shortCode);

        log.info(
                "Short URL deleted successfully: {}",
                shortCode
        );
    }


    /**
     * ========================================================================
     * Redis Cache - GET
     * ========================================================================
     */
    private Url getFromCache(String shortCode) {

        Cache cache = cacheManager.getCache(
                CacheConstants.URL_CACHE
        );

        if (cache == null) {

            log.warn(
                    "Redis cache '{}' is not available",
                    CacheConstants.URL_CACHE
            );

            return null;
        }

        Cache.ValueWrapper valueWrapper = cache.get(shortCode);

        if (valueWrapper == null) {

            return null;
        }

        Object value = valueWrapper.get();

        if (value instanceof Url) {

            return (Url) value;
        }

        log.warn(
                "Unexpected value found in Redis cache for key: {}. Type={}",
                shortCode,
                value == null
                        ? "null"
                        : value.getClass().getName()
        );

        return null;
    }


    /**
     * ========================================================================
     * Redis Cache - PUT
     * ========================================================================
     */
    private void putInCache(Url url) {

        if (url == null || url.getShortCode() == null) {

            return;
        }

        Cache cache = cacheManager.getCache(
                CacheConstants.URL_CACHE
        );

        if (cache == null) {

            log.error(
                    "Redis cache '{}' is NOT available",
                    CacheConstants.URL_CACHE
            );

            return;
        }

        log.info(
                "Putting URL into cache. Cache={}, Key={}",
                CacheConstants.URL_CACHE,
                url.getShortCode()
        );

        cache.put(
                url.getShortCode(),
                url
        );

        log.info(
                "URL cached in Redis. Code={}",
                url.getShortCode()
        );
    }


    /**
     * ========================================================================
     * Redis Cache - EVICT
     * ========================================================================
     */
    private void evictFromCache(String shortCode) {

        if (shortCode == null) {

            return;
        }

        Cache cache = cacheManager.getCache(
                CacheConstants.URL_CACHE
        );

        if (cache == null) {

            log.warn(
                    "Redis cache '{}' is not available",
                    CacheConstants.URL_CACHE
            );

            return;
        }

        cache.evict(shortCode);

        log.info(
                "URL removed from Redis cache. Code={}",
                shortCode
        );
    }
}