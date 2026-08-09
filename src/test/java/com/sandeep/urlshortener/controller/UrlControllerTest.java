package com.sandeep.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.sandeep.urlshortener.dto.request.CreateShortUrlRequest;
import com.sandeep.urlshortener.dto.response.CreateShortUrlResponse;
import com.sandeep.urlshortener.dto.response.UrlStatsResponse;
import com.sandeep.urlshortener.exception.DuplicateShortCodeException;
import com.sandeep.urlshortener.exception.ResourceNotFoundException;
import com.sandeep.urlshortener.service.RateLimitService;
import com.sandeep.urlshortener.service.UrlService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web MVC tests for UrlController.
 *
 * These tests verify the HTTP layer without starting the
 * complete application or connecting to PostgreSQL/Redis.
 */
@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlService service;

        @MockBean
        private RateLimitService rateLimitService;
        @BeforeEach
        void setUp() {
        when(rateLimitService.isAllowed(
                anyString(),
                anyInt(),
                anyLong()
        )).thenReturn(true);

        when(rateLimitService.getRemainingRequests(
                anyString(),
                anyInt()
        )).thenReturn(9L);
        }
    /**
     * Tests successful short URL creation.
     */
    @Test
    @DisplayName("POST /api/v1/urls - should create short URL")
    void create_shouldReturnCreated() throws Exception {

        // Arrange
        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl("https://www.google.com")
                        .build();

        CreateShortUrlResponse response =
                CreateShortUrlResponse.builder()
                        .shortCode("Ab12Cd")
                        .originalUrl("https://www.google.com")
                        .shortUrl("http://localhost:8080/Ab12Cd")
                        .createdAt(LocalDateTime.now())
                        .clickCount(0L)
                        .build();

        when(service.createShortUrl(
                any(CreateShortUrlRequest.class)
        )).thenReturn(response);

        // Act & Assert
        mockMvc.perform(
                        post("/api/v1/urls")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.shortCode")
                                .value("Ab12Cd")
                )
                .andExpect(
                        jsonPath("$.originalUrl")
                                .value("https://www.google.com")
                )
                .andExpect(
                        jsonPath("$.shortUrl")
                                .value(
                                        "http://localhost:8080/Ab12Cd"
                                )
                )
                .andExpect(
                        jsonPath("$.clickCount")
                                .value(0)
                );

        verify(service, times(1))
                .createShortUrl(
                        any(CreateShortUrlRequest.class)
                );
    }

    /**
     * Tests validation when the original URL is missing.
     */
    @Test
    @DisplayName("POST /api/v1/urls - should return 400 for missing URL")
    void create_shouldReturnBadRequestWhenUrlIsMissing()
            throws Exception {

        // Arrange
        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl("")
                        .build();

        // Act & Assert
        mockMvc.perform(
                        post("/api/v1/urls")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Validation Failed")
                )
                .andExpect(
                        jsonPath("$.message")
                                .exists()
                );

        verify(service, never())
                .createShortUrl(
                        any(CreateShortUrlRequest.class)
                );
    }

    /**
     * Tests validation when an invalid URL is supplied.
     */
    @Test
    @DisplayName("POST /api/v1/urls - should return 400 for invalid URL")
    void create_shouldReturnBadRequestForInvalidUrl()
            throws Exception {

        // Arrange
        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl("not-a-valid-url")
                        .build();

        // Act & Assert
        mockMvc.perform(
                        post("/api/v1/urls")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Validation Failed")
                )
                .andExpect(
                        jsonPath("$.message")
                                .exists()
                );

        verify(service, never())
                .createShortUrl(
                        any(CreateShortUrlRequest.class)
                );
    }

    /**
     * Tests successful retrieval of URL statistics.
     */
    @Test
    @DisplayName(
            "GET /api/v1/urls/{shortCode}/stats - should return statistics"
    )
    void getStatistics_shouldReturnOk()
            throws Exception {

        // Arrange
        String shortCode = "Ab12Cd";

        UrlStatsResponse response =
                UrlStatsResponse.builder()
                        .shortCode(shortCode)
                        .originalUrl("https://www.google.com")
                        .createdAt(
                                LocalDateTime.now().minusHours(2)
                        )
                        .expiresAt(
                                LocalDateTime.now().plusHours(2)
                        )
                        .clickCount(25L)
                        .build();

        when(service.getStatistics(shortCode))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(
                        get(
                                "/api/v1/urls/{shortCode}/stats",
                                shortCode
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.shortCode")
                                .value(shortCode)
                )
                .andExpect(
                        jsonPath("$.originalUrl")
                                .value(
                                        "https://www.google.com"
                                )
                )
                .andExpect(
                        jsonPath("$.clickCount")
                                .value(25)
                );

        verify(service)
                .getStatistics(shortCode);
    }

    /**
     * Tests statistics retrieval for a non-existent short code.
     */
    @Test
    @DisplayName(
            "GET /api/v1/urls/{shortCode}/stats - should return 404"
    )
    void getStatistics_shouldReturnNotFoundWhenShortCodeDoesNotExist()
            throws Exception {

        // Arrange
        String shortCode = "Unknown";

        when(service.getStatistics(shortCode))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Short URL '" +
                                        shortCode +
                                        "' not found"
                        )
                );

        // Act & Assert
        mockMvc.perform(
                        get(
                                "/api/v1/urls/{shortCode}/stats",
                                shortCode
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Short URL 'Unknown' not found"
                                )
                );

        verify(service)
                .getStatistics(shortCode);
    }

    /**
     * Tests successful deletion of a short URL.
     */
    @Test
    @DisplayName(
            "DELETE /api/v1/urls/{shortCode} - should return 204"
    )
    void delete_shouldReturnNoContent()
            throws Exception {

        // Arrange
        String shortCode = "Ab12Cd";

        doNothing()
                .when(service)
                .deleteShortUrl(shortCode);

        // Act & Assert
        mockMvc.perform(
                        delete(
                                "/api/v1/urls/{shortCode}",
                                shortCode
                        )
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service, times(1))
                .deleteShortUrl(shortCode);
    }

    /**
     * Tests deletion when the short URL does not exist.
     */
    @Test
    @DisplayName(
            "DELETE /api/v1/urls/{shortCode} - should return 404"
    )
    void delete_shouldReturnNotFoundWhenShortCodeDoesNotExist()
            throws Exception {

        // Arrange
        String shortCode = "Unknown";

        doThrow(
                new ResourceNotFoundException(
                        "Short URL '" +
                                shortCode +
                                "' not found"
                )
        )
                .when(service)
                .deleteShortUrl(shortCode);

        // Act & Assert
        mockMvc.perform(
                        delete(
                                "/api/v1/urls/{shortCode}",
                                shortCode
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Short URL 'Unknown' not found"
                                )
                );

        verify(service)
                .deleteShortUrl(shortCode);
    }

    /**
     * Tests handling of a duplicate short-code exception.
     */
    @Test
    @DisplayName(
            "POST /api/v1/urls - should return 409 for duplicate code"
    )
    void create_shouldReturnConflictWhenDuplicateShortCode()
            throws Exception {

        // Arrange
        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl(
                                "https://www.google.com"
                        )
                        .build();

        when(
                service.createShortUrl(
                        any(CreateShortUrlRequest.class)
                )
        )
                .thenThrow(
                        new DuplicateShortCodeException(
                                "Short code already exists"
                        )
                );

        // Act & Assert
        mockMvc.perform(
                        post("/api/v1/urls")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Short code already exists"
                                )
                );

        verify(service)
                .createShortUrl(
                        any(CreateShortUrlRequest.class)
                );
    }

    /**
     * Tests unexpected exceptions from the service layer.
     */
    @Test
    @DisplayName(
            "POST /api/v1/urls - should return 500 for unexpected error"
    )
    void create_shouldReturnInternalServerErrorForUnexpectedException()
            throws Exception {

        // Arrange
        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl(
                                "https://www.google.com"
                        )
                        .build();

        when(
                service.createShortUrl(
                        any(CreateShortUrlRequest.class)
                )
        )
                .thenThrow(
                        new RuntimeException(
                                "Database connection failed"
                        )
                );

        // Act & Assert
        mockMvc.perform(
                        post("/api/v1/urls")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(
                        status().isInternalServerError()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(500)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Internal Server Error"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An unexpected error occurred"
                                )
                );

        verify(service)
                .createShortUrl(
                        any(CreateShortUrlRequest.class)
                );
    }
}