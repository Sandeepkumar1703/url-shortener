package com.sandeep.urlshortener.service.impl;

import com.sandeep.urlshortener.cache.CacheConstants;
import com.sandeep.urlshortener.dto.request.CreateShortUrlRequest;
import com.sandeep.urlshortener.dto.response.CreateShortUrlResponse;
import com.sandeep.urlshortener.dto.response.UrlStatsResponse;
import com.sandeep.urlshortener.entity.Url;
import com.sandeep.urlshortener.exception.ResourceNotFoundException;
import com.sandeep.urlshortener.exception.UrlExpiredException;
import com.sandeep.urlshortener.repository.UrlRepository;
import com.sandeep.urlshortener.util.ShortCodeGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Unit tests for {@link UrlServiceImpl}.
 *
 * <p>
 * These tests verify the service layer in isolation.
 * They do not start the Spring application context and do not
 * connect to PostgreSQL or Redis.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    private UrlRepository repository;

    @Mock
    private ShortCodeGenerator generator;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private UrlServiceImpl urlService;

    private static final String SHORT_CODE = "Ab12Cd";

    private static final String ORIGINAL_URL =
            "https://www.google.com";


    // ========================================================================
    // CREATE SHORT URL
    // ========================================================================

    /**
     * Verifies that a short URL is successfully created.
     */
    @Test
    void createShortUrl_shouldCreateAndReturnShortUrl() {

        // Arrange
        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl(ORIGINAL_URL)
                        .expiresAt(null)
                        .build();

        Url savedUrl =
                Url.builder()
                        .id(1L)
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(LocalDateTime.now())
                        .clickCount(0L)
                        .build();

        when(generator.generate())
                .thenReturn(SHORT_CODE);

        when(repository.existsByShortCode(SHORT_CODE))
                .thenReturn(false);

        when(repository.save(any(Url.class)))
                .thenReturn(savedUrl);

        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(cache);

        // Act
        CreateShortUrlResponse response =
                urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);

        assertEquals(
                SHORT_CODE,
                response.getShortCode()
        );

        assertEquals(
                ORIGINAL_URL,
                response.getOriginalUrl()
        );

        assertEquals(
                "http://localhost:8080/" + SHORT_CODE,
                response.getShortUrl()
        );

        assertEquals(
                0L,
                response.getClickCount()
        );

        // Verify interactions
        verify(generator)
                .generate();

        verify(repository)
                .existsByShortCode(SHORT_CODE);

        verify(repository)
                .save(any(Url.class));

        verify(cacheManager)
                .getCache(CacheConstants.URL_CACHE);

        verify(cache)
                .put(SHORT_CODE, savedUrl);
    }


    /**
     * Verifies that another short code is generated when
     * the first generated code already exists.
     */
    @Test
    void createShortUrl_shouldGenerateAnotherCodeWhenCodeAlreadyExists() {

        // Arrange
        String firstCode = "Ab12Cd";
        String secondCode = "Xy98Zq";

        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl(ORIGINAL_URL)
                        .build();

        Url savedUrl =
                Url.builder()
                        .id(1L)
                        .shortCode(secondCode)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(LocalDateTime.now())
                        .clickCount(0L)
                        .build();

        when(generator.generate())
                .thenReturn(firstCode)
                .thenReturn(secondCode);

        when(repository.existsByShortCode(firstCode))
                .thenReturn(true);

        when(repository.existsByShortCode(secondCode))
                .thenReturn(false);

        when(repository.save(any(Url.class)))
                .thenReturn(savedUrl);

        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(cache);

        // Act
        CreateShortUrlResponse response =
                urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);

        assertEquals(
                secondCode,
                response.getShortCode()
        );

        assertEquals(
                ORIGINAL_URL,
                response.getOriginalUrl()
        );

        // Verify generator was called twice
        verify(generator, times(2))
                .generate();

        verify(repository)
                .existsByShortCode(firstCode);

        verify(repository)
                .existsByShortCode(secondCode);

        verify(repository)
                .save(any(Url.class));

        verify(cacheManager)
                .getCache(CacheConstants.URL_CACHE);

        verify(cache)
                .put(secondCode, savedUrl);
    }


    // ========================================================================
    // GET ORIGINAL URL - CACHE MISS
    // ========================================================================

    /**
     * Verifies that a valid short URL is retrieved from PostgreSQL
     * when Redis has a cache miss.
     */
    @Test
    void getOriginalUrl_shouldReturnOriginalUrlAndIncrementClickCount() {

        // Arrange
        Url url =
                Url.builder()
                        .id(1L)
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(LocalDateTime.now().minusHours(1))
                        .expiresAt(LocalDateTime.now().plusHours(1))
                        .clickCount(5L)
                        .build();

        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(cache);

        when(cache.get(SHORT_CODE))
                .thenReturn(null);

        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.of(url));

        when(repository.save(url))
                .thenReturn(url);

        // Act
        String result =
                urlService.getOriginalUrl(SHORT_CODE);

        // Assert
        assertEquals(
                ORIGINAL_URL,
                result
        );

        assertEquals(
                6L,
                url.getClickCount()
        );

        // Cache is accessed:
        // 1. getFromCache()
        // 2. putInCache() after repository save
        verify(cacheManager, atLeastOnce())
                .getCache(CacheConstants.URL_CACHE);

        verify(cache)
                .get(SHORT_CODE);

        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository)
                .save(url);

        verify(cache)
                .put(SHORT_CODE, url);
    }


    // ========================================================================
    // GET ORIGINAL URL - CACHE HIT
    // ========================================================================

    /**
     * Verifies that a valid URL is returned directly from Redis
     * and the click count is incremented.
     */
    @Test
    void getOriginalUrl_shouldReturnOriginalUrlFromCache() {

        // Arrange
        Url url =
                Url.builder()
                        .id(1L)
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(LocalDateTime.now().minusHours(1))
                        .expiresAt(LocalDateTime.now().plusHours(1))
                        .clickCount(10L)
                        .build();

        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(cache);

        when(cache.get(SHORT_CODE))
                .thenReturn(new Cache.ValueWrapper() {
                    @Override
                    public Object get() {
                        return url;
                    }
                });

        when(repository.save(url))
                .thenReturn(url);

        // Act
        String result =
                urlService.getOriginalUrl(SHORT_CODE);

        // Assert
        assertEquals(
                ORIGINAL_URL,
                result
        );

        assertEquals(
                11L,
                url.getClickCount()
        );

        verify(cacheManager, atLeastOnce())
                .getCache(CacheConstants.URL_CACHE);

        verify(cache)
                .get(SHORT_CODE);

        verify(repository)
                .save(url);

        verify(cache)
                .put(SHORT_CODE, url);

        // PostgreSQL lookup should NOT happen on cache hit
        verify(repository, never())
                .findByShortCode(SHORT_CODE);
    }


    // ========================================================================
    // GET ORIGINAL URL - NOT FOUND
    // ========================================================================

    /**
     * Verifies that ResourceNotFoundException is thrown
     * when the short code does not exist.
     */
    @Test
    void getOriginalUrl_shouldThrowExceptionWhenShortCodeDoesNotExist() {

        // Arrange
        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(cache);

        when(cache.get(SHORT_CODE))
                .thenReturn(null);

        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> urlService.getOriginalUrl(SHORT_CODE)
                );

        assertEquals(
                "Short URL not found: " + SHORT_CODE,
                exception.getMessage()
        );

        verify(cacheManager)
                .getCache(CacheConstants.URL_CACHE);

        verify(cache)
                .get(SHORT_CODE);

        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository, never())
                .save(any(Url.class));

        verify(cache, never())
                .put(any(), any());
    }


    // ========================================================================
    // GET ORIGINAL URL - EXPIRED
    // ========================================================================

    /**
     * Verifies that an expired URL cannot be accessed.
     */
    @Test
    void getOriginalUrl_shouldThrowExceptionWhenUrlIsExpired() {

        // Arrange
        Url url =
                Url.builder()
                        .id(1L)
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .expiresAt(LocalDateTime.now().minusHours(1))
                        .clickCount(5L)
                        .build();

        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(cache);

        when(cache.get(SHORT_CODE))
                .thenReturn(null);

        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.of(url));

        // Act & Assert
        UrlExpiredException exception =
                assertThrows(
                        UrlExpiredException.class,
                        () -> urlService.getOriginalUrl(SHORT_CODE)
                );

        assertEquals(
                "Short URL has expired: " + SHORT_CODE,
                exception.getMessage()
        );

        // Click count must remain unchanged
        assertEquals(
                5L,
                url.getClickCount()
        );

        verify(cacheManager, atLeastOnce())
                .getCache(CacheConstants.URL_CACHE);

        verify(cache)
                .get(SHORT_CODE);

        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository, never())
                .save(any(Url.class));

        // Expired URL is removed from cache
        verify(cache)
                .evict(SHORT_CODE);
    }


    /**
     * Verifies that an expired URL already present in Redis
     * is evicted and an exception is thrown.
     */
    @Test
    void getOriginalUrl_shouldThrowExceptionWhenCachedUrlIsExpired() {

        // Arrange
        Url expiredUrl =
                Url.builder()
                        .id(1L)
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .expiresAt(LocalDateTime.now().minusHours(1))
                        .clickCount(5L)
                        .build();

        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(cache);

        when(cache.get(SHORT_CODE))
                .thenReturn(new Cache.ValueWrapper() {
                    @Override
                    public Object get() {
                        return expiredUrl;
                    }
                });

        // Act & Assert
        UrlExpiredException exception =
                assertThrows(
                        UrlExpiredException.class,
                        () -> urlService.getOriginalUrl(SHORT_CODE)
                );

        assertEquals(
                "Short URL has expired: " + SHORT_CODE,
                exception.getMessage()
        );

        assertEquals(
                5L,
                expiredUrl.getClickCount()
        );

        verify(cache)
                .get(SHORT_CODE);

        verify(cache)
                .evict(SHORT_CODE);

        verify(repository, never())
                .findByShortCode(SHORT_CODE);

        verify(repository, never())
                .save(any(Url.class));
    }


    // ========================================================================
    // GET STATISTICS
    // ========================================================================

    /**
     * Verifies that statistics are returned correctly.
     */
    @Test
    void getStatistics_shouldReturnUrlStatistics() {

        // Arrange
        LocalDateTime createdAt =
                LocalDateTime.now().minusHours(2);

        LocalDateTime expiresAt =
                LocalDateTime.now().plusHours(2);

        Url url =
                Url.builder()
                        .id(1L)
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(createdAt)
                        .expiresAt(expiresAt)
                        .clickCount(25L)
                        .build();

        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.of(url));

        // Act
        UrlStatsResponse response =
                urlService.getStatistics(SHORT_CODE);

        // Assert
        assertNotNull(response);

        assertEquals(
                SHORT_CODE,
                response.getShortCode()
        );

        assertEquals(
                ORIGINAL_URL,
                response.getOriginalUrl()
        );

        assertEquals(
                createdAt,
                response.getCreatedAt()
        );

        assertEquals(
                expiresAt,
                response.getExpiresAt()
        );

        assertEquals(
                25L,
                response.getClickCount()
        );

        verify(repository)
                .findByShortCode(SHORT_CODE);
    }


    /**
     * Verifies that getStatistics() throws
     * ResourceNotFoundException when the short code
     * does not exist.
     */
    @Test
    void getStatistics_shouldThrowExceptionWhenShortCodeDoesNotExist() {

        // Arrange
        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> urlService.getStatistics(SHORT_CODE)
                );

        assertEquals(
                "Short URL '" + SHORT_CODE + "' not found",
                exception.getMessage()
        );

        verify(repository)
                .findByShortCode(SHORT_CODE);
    }


    // ========================================================================
    // DELETE SHORT URL
    // ========================================================================

    /**
     * Verifies that an existing short URL is deleted
     * from PostgreSQL and Redis.
     */
    @Test
    void deleteShortUrl_shouldDeleteExistingUrl() {

        // Arrange
        Url url =
                Url.builder()
                        .id(1L)
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(LocalDateTime.now())
                        .clickCount(0L)
                        .build();

        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.of(url));

        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(cache);

        // Act
        urlService.deleteShortUrl(SHORT_CODE);

        // Assert
        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository)
                .delete(url);

        verify(cacheManager)
                .getCache(CacheConstants.URL_CACHE);

        verify(cache)
                .evict(SHORT_CODE);
    }


    /**
     * Verifies that deleteShortUrl() throws
     * ResourceNotFoundException when the short code
     * does not exist.
     */
    @Test
    void deleteShortUrl_shouldThrowExceptionWhenShortCodeDoesNotExist() {

        // Arrange
        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> urlService.deleteShortUrl(SHORT_CODE)
                );

        assertEquals(
                "Short URL '" + SHORT_CODE + "' not found",
                exception.getMessage()
        );

        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository, never())
                .delete(any(Url.class));

        // Cache should not be accessed
        verify(cacheManager, never())
                .getCache(CacheConstants.URL_CACHE);
    }


    // ========================================================================
    // CACHE UNAVAILABLE
    // ========================================================================

    /**
     * Verifies that creating a short URL still succeeds
     * when Redis cache is unavailable.
     */
    @Test
    void createShortUrl_shouldStillWorkWhenCacheIsUnavailable() {

        // Arrange
        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl(ORIGINAL_URL)
                        .build();

        Url savedUrl =
                Url.builder()
                        .id(1L)
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(LocalDateTime.now())
                        .clickCount(0L)
                        .build();

        when(generator.generate())
                .thenReturn(SHORT_CODE);

        when(repository.existsByShortCode(SHORT_CODE))
                .thenReturn(false);

        when(repository.save(any(Url.class)))
                .thenReturn(savedUrl);

        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(null);

        // Act
        CreateShortUrlResponse response =
                urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);

        assertEquals(
                SHORT_CODE,
                response.getShortCode()
        );

        assertEquals(
                ORIGINAL_URL,
                response.getOriginalUrl()
        );

        verify(repository)
                .save(any(Url.class));

        verify(cacheManager)
                .getCache(CacheConstants.URL_CACHE);
    }


    /**
     * Verifies that resolving a URL still works when Redis
     * is unavailable.
     */
    @Test
    void getOriginalUrl_shouldWorkWhenCacheIsUnavailable() {

        // Arrange
        Url url =
                Url.builder()
                        .id(1L)
                        .shortCode(SHORT_CODE)
                        .originalUrl(ORIGINAL_URL)
                        .createdAt(LocalDateTime.now().minusHours(1))
                        .expiresAt(LocalDateTime.now().plusHours(1))
                        .clickCount(5L)
                        .build();

        when(cacheManager.getCache(CacheConstants.URL_CACHE))
                .thenReturn(null);

        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.of(url));

        when(repository.save(url))
                .thenReturn(url);

        // Act
        String result =
                urlService.getOriginalUrl(SHORT_CODE);

        // Assert
        assertEquals(
                ORIGINAL_URL,
                result
        );

        assertEquals(
                6L,
                url.getClickCount()
        );

        verify(cacheManager, atLeastOnce())
                .getCache(CacheConstants.URL_CACHE);

        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository)
                .save(url);
    }
}