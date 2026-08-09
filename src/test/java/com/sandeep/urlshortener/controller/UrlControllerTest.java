package com.sandeep.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.sandeep.urlshortener.dto.request.CreateShortUrlRequest;
import com.sandeep.urlshortener.dto.response.CreateShortUrlResponse;
import com.sandeep.urlshortener.dto.response.UrlStatsResponse;
import com.sandeep.urlshortener.exception.DuplicateShortCodeException;
import com.sandeep.urlshortener.exception.ResourceNotFoundException;
import com.sandeep.urlshortener.exception.UrlExpiredException;
import com.sandeep.urlshortener.service.UrlService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web MVC tests for {@link UrlController}.
 *
 * <p>This test class verifies the REST API layer without starting
 * the complete application or connecting to PostgreSQL.</p>
 *
 * <p>The {@link UrlService} is mocked because the service layer
 * is tested separately in {@code UrlServiceImplTest}.</p>
 */
@WebMvcTest(UrlController.class)
class UrlControllerTest {

    /**
     * MockMvc is used to send HTTP requests to the controller.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * ObjectMapper converts Java objects into JSON.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Mocked URL service.
     *
     * <p>The controller communicates with this mock instead
     * of the real service implementation.</p>
     */
    @MockBean
    private UrlService service;

    /**
     * Tests successful short URL creation.
     *
     * <p>Expected HTTP status: 201 Created.</p>
     */
    @Test
    @DisplayName("POST /api/v1/urls - should create short URL")
    void create_shouldReturnCreated() throws Exception {

        // Arrange
        CreateShortUrlRequest request = CreateShortUrlRequest.builder()
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

        when(service.createShortUrl(any(CreateShortUrlRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(
                        post("/api/v1/urls")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.shortCode").value("Ab12Cd"))
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://www.google.com"))
                .andExpect(jsonPath("$.shortUrl")
                        .value("http://localhost:8080/Ab12Cd"))
                .andExpect(jsonPath("$.clickCount").value(0));

        // Verify that the controller called the service once.
        verify(service, times(1))
                .createShortUrl(any(CreateShortUrlRequest.class));
    }

    /**
     * Tests validation when the original URL is missing.
     *
     * <p>Expected HTTP status: 400 Bad Request.</p>
     *
     * <p>The service should not be called because validation
     * fails at the controller layer.</p>
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Validation Failed"))
                .andExpect(jsonPath("$.message").exists());

        // Service must not be called when validation fails.
        verify(service, never())
                .createShortUrl(any(CreateShortUrlRequest.class));
    }

    /**
     * Tests validation when an invalid URL is supplied.
     *
     * <p>Expected HTTP status: 400 Bad Request.</p>
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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Validation Failed"))
                .andExpect(jsonPath("$.message").exists());

        // Service must not be called.
        verify(service, never())
                .createShortUrl(any(CreateShortUrlRequest.class));
    }

    /**
     * Tests successful redirection to the original URL.
     *
     * <p>Expected HTTP status: 302 Found.</p>
     *
     * <p>The original URL must be returned in the Location header.</p>
     */
    @Test
    @DisplayName("GET /api/v1/urls/{shortCode} - should redirect")
    void redirect_shouldReturnFound() throws Exception {

        // Arrange
        String shortCode = "Ab12Cd";
        String originalUrl = "https://www.google.com";

        when(service.getOriginalUrl(shortCode))
                .thenReturn(originalUrl);

        // Act & Assert
        mockMvc.perform(
                        get("/api/v1/urls/{shortCode}", shortCode)
                )
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        originalUrl
                ));

        // Verify service interaction.
        verify(service, times(1))
                .getOriginalUrl(shortCode);
    }

    /**
     * Tests redirection when the requested short URL does not exist.
     *
     * <p>Expected HTTP status: 404 Not Found.</p>
     */
    @Test
    @DisplayName("GET /api/v1/urls/{shortCode} - should return 404")
    void redirect_shouldReturnNotFoundWhenShortCodeDoesNotExist()
            throws Exception {

        // Arrange
        String shortCode = "Unknown";

        when(service.getOriginalUrl(shortCode))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Short URL '" + shortCode + "' not found"
                        )
                );

        // Act & Assert
        mockMvc.perform(
                        get("/api/v1/urls/{shortCode}", shortCode)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL 'Unknown' not found"));

        verify(service).getOriginalUrl(shortCode);
    }

    /**
     * Tests redirection when the short URL has expired.
     *
     * <p>Expected HTTP status: 410 Gone.</p>
     */
    @Test
    @DisplayName("GET /api/v1/urls/{shortCode} - should return 410 when expired")
    void redirect_shouldReturnGoneWhenUrlExpired()
            throws Exception {

        // Arrange
        String shortCode = "Ab12Cd";

        when(service.getOriginalUrl(shortCode))
                .thenThrow(
                        new UrlExpiredException(
                                "Short URL '" + shortCode + "' has expired"
                        )
                );

        // Act & Assert
        mockMvc.perform(
                        get("/api/v1/urls/{shortCode}", shortCode)
                )
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error")
                        .value("URL Expired"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL 'Ab12Cd' has expired"));

        verify(service).getOriginalUrl(shortCode);
    }

    /**
     * Tests successful retrieval of URL statistics.
     *
     * <p>Expected HTTP status: 200 OK.</p>
     */
    @Test
    @DisplayName("GET /api/v1/urls/{shortCode}/stats - should return statistics")
    void getStatistics_shouldReturnOk() throws Exception {

        // Arrange
        String shortCode = "Ab12Cd";

        UrlStatsResponse response =
                UrlStatsResponse.builder()
                        .shortCode(shortCode)
                        .originalUrl("https://www.google.com")
                        .createdAt(LocalDateTime.now().minusHours(2))
                        .expiresAt(LocalDateTime.now().plusHours(2))
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
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.shortCode")
                        .value(shortCode))
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://www.google.com"))
                .andExpect(jsonPath("$.clickCount")
                        .value(25));

        verify(service).getStatistics(shortCode);
    }

    /**
     * Tests statistics retrieval for a non-existent short code.
     *
     * <p>Expected HTTP status: 404 Not Found.</p>
     */
    @Test
    @DisplayName("GET /api/v1/urls/{shortCode}/stats - should return 404")
    void getStatistics_shouldReturnNotFoundWhenShortCodeDoesNotExist()
            throws Exception {

        // Arrange
        String shortCode = "Unknown";

        when(service.getStatistics(shortCode))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Short URL '" + shortCode + "' not found"
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
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL 'Unknown' not found"));

        verify(service).getStatistics(shortCode);
    }

    /**
     * Tests successful deletion of a short URL.
     *
     * <p>Expected HTTP status: 204 No Content.</p>
     */
    @Test
    @DisplayName("DELETE /api/v1/urls/{shortCode} - should return 204")
    void delete_shouldReturnNoContent() throws Exception {

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
     *
     * <p>Expected HTTP status: 404 Not Found.</p>
     */
    @Test
    @DisplayName("DELETE /api/v1/urls/{shortCode} - should return 404")
    void delete_shouldReturnNotFoundWhenShortCodeDoesNotExist()
            throws Exception {

        // Arrange
        String shortCode = "Unknown";

        doThrow(
                new ResourceNotFoundException(
                        "Short URL '" + shortCode + "' not found"
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
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL 'Unknown' not found"));

        verify(service).deleteShortUrl(shortCode);
    }

    /**
     * Tests handling of a duplicate short-code exception.
     *
     * <p>Expected HTTP status: 409 Conflict.</p>
     */
    @Test
    @DisplayName("POST /api/v1/urls - should return 409 for duplicate code")
    void create_shouldReturnConflictWhenDuplicateShortCode()
            throws Exception {

        // Arrange
        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl("https://www.google.com")
                        .build();

        when(service.createShortUrl(any(CreateShortUrlRequest.class)))
                .thenThrow(
                        new DuplicateShortCodeException(
                                "Short code already exists"
                        )
                );

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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Short code already exists"));

        verify(service)
                .createShortUrl(any(CreateShortUrlRequest.class));
    }

    /**
     * Tests unexpected exceptions from the service layer.
     *
     * <p>Expected HTTP status: 500 Internal Server Error.</p>
     *
     * <p>The actual internal exception message must not be exposed
     * to the API client.</p>
     */
    @Test
    @DisplayName("POST /api/v1/urls - should return 500 for unexpected error")
    void create_shouldReturnInternalServerErrorForUnexpectedException()
            throws Exception {

        // Arrange
        CreateShortUrlRequest request =
                CreateShortUrlRequest.builder()
                        .originalUrl("https://www.google.com")
                        .build();

        when(service.createShortUrl(any(CreateShortUrlRequest.class)))
                .thenThrow(
                        new RuntimeException("Database connection failed")
                );

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
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error")
                        .value("Internal Server Error"))
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred"));

        verify(service)
                .createShortUrl(any(CreateShortUrlRequest.class));
    }
}