package com.sandeep.urlshortener.service;

import com.sandeep.urlshortener.dto.request.CreateShortUrlRequest;
import com.sandeep.urlshortener.dto.response.CreateShortUrlResponse;
import com.sandeep.urlshortener.dto.response.UrlStatsResponse;
import com.sandeep.urlshortener.entity.Url;
import com.sandeep.urlshortener.exception.ResourceNotFoundException;
import com.sandeep.urlshortener.exception.UrlExpiredException;
import com.sandeep.urlshortener.repository.UrlRepository;
import com.sandeep.urlshortener.service.impl.UrlServiceImpl;
import com.sandeep.urlshortener.util.ShortCodeGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UrlServiceImpl.
 *
 * These tests verify the service layer in isolation without
 * starting the Spring application context or connecting to
 * the PostgreSQL database.
 *
 * UrlRepository and ShortCodeGenerator are mocked using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    private UrlRepository repository;

    @Mock
    private ShortCodeGenerator generator;

    @InjectMocks
    private UrlServiceImpl urlService;

    private static final String SHORT_CODE = "Ab12Cd";

    private static final String ORIGINAL_URL =
            "https://www.google.com";

    /**
     * Verifies that a short URL is successfully created.
     */
    @Test
    void createShortUrl_shouldCreateAndReturnShortUrl() {

        // Arrange
        CreateShortUrlRequest request = CreateShortUrlRequest.builder()
                .originalUrl(ORIGINAL_URL)
                .expiresAt(null)
                .build();

        Url savedUrl = Url.builder()
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
        verify(generator).generate();

        verify(repository)
                .existsByShortCode(SHORT_CODE);

        verify(repository)
                .save(any(Url.class));
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

        Url savedUrl = Url.builder()
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

        // Act
        CreateShortUrlResponse response =
                urlService.createShortUrl(request);

        // Assert
        assertNotNull(response);

        assertEquals(
                secondCode,
                response.getShortCode()
        );

        // Generator should have been called twice
        verify(generator, times(2))
                .generate();

        verify(repository)
                .existsByShortCode(firstCode);

        verify(repository)
                .existsByShortCode(secondCode);

        verify(repository)
                .save(any(Url.class));
    }

    /**
     * Verifies that a valid short URL returns the original URL
     * and increments the click count.
     */
    @Test
    void getOriginalUrl_shouldReturnOriginalUrlAndIncrementClickCount() {

        // Arrange
        Url url = Url.builder()
                .id(1L)
                .shortCode(SHORT_CODE)
                .originalUrl(ORIGINAL_URL)
                .createdAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .clickCount(5L)
                .build();

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

        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository)
                .save(url);
    }

    /**
     * Verifies that ResourceNotFoundException is thrown
     * when the short code does not exist.
     */
    @Test
    void getOriginalUrl_shouldThrowExceptionWhenShortCodeDoesNotExist() {

        // Arrange
        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> urlService.getOriginalUrl(SHORT_CODE)
                );

        assertEquals(
                "Short URL '" + SHORT_CODE + "' not found",
                exception.getMessage()
        );

        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository, never())
                .save(any(Url.class));
    }

    /**
     * Verifies that an expired URL cannot be accessed.
     */
    @Test
    void getOriginalUrl_shouldThrowExceptionWhenUrlIsExpired() {

        // Arrange
        Url url = Url.builder()
                .id(1L)
                .shortCode(SHORT_CODE)
                .originalUrl(ORIGINAL_URL)
                .createdAt(LocalDateTime.now().minusDays(2))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .clickCount(5L)
                .build();

        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.of(url));

        // Act & Assert
        UrlExpiredException exception =
                assertThrows(
                        UrlExpiredException.class,
                        () -> urlService.getOriginalUrl(SHORT_CODE)
                );

        assertEquals(
                "Short URL '" + SHORT_CODE + "' has expired",
                exception.getMessage()
        );

        // Click count must remain unchanged
        assertEquals(
                5L,
                url.getClickCount()
        );

        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository, never())
                .save(any(Url.class));
    }

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

        Url url = Url.builder()
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

    /**
     * Verifies that an existing short URL is deleted.
     */
    @Test
    void deleteShortUrl_shouldDeleteExistingUrl() {

        // Arrange
        Url url = Url.builder()
                .id(1L)
                .shortCode(SHORT_CODE)
                .originalUrl(ORIGINAL_URL)
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .build();

        when(repository.findByShortCode(SHORT_CODE))
                .thenReturn(Optional.of(url));

        // Act
        urlService.deleteShortUrl(SHORT_CODE);

        // Assert
        verify(repository)
                .findByShortCode(SHORT_CODE);

        verify(repository)
                .delete(url);
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
    }
}