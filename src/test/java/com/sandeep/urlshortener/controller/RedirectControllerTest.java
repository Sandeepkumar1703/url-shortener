package com.sandeep.urlshortener.controller;

import com.sandeep.urlshortener.exception.GlobalExceptionHandler;
import com.sandeep.urlshortener.exception.ResourceNotFoundException;
import com.sandeep.urlshortener.exception.UrlExpiredException;
import com.sandeep.urlshortener.interceptor.RateLimitInterceptor;
import com.sandeep.urlshortener.service.RateLimitService;
import com.sandeep.urlshortener.service.UrlService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.web.servlet.HandlerInterceptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
@Import(GlobalExceptionHandler.class)
class RedirectControllerTest {

@Autowired
private MockMvc mockMvc;

@MockBean
private UrlService service;

@MockBean
private RateLimitInterceptor rateLimitInterceptor;

@MockBean
private RateLimitService rateLimitService;

/**
 * The RateLimitInterceptor is mocked.
 *
 * Mockito returns false by default for boolean methods.
 * Spring's HandlerInterceptor#preHandle() must return true
 * for the controller to execute.
 */
@BeforeEach
void setUp() throws Exception {

    when(
            rateLimitInterceptor.preHandle(
                    any(HttpServletRequest.class),
                    any(HttpServletResponse.class),
                    any(Object.class)
            )
    ).thenReturn(true);
}

/**
 * Tests successful redirection.
 */
@Test
@DisplayName("GET /{shortCode} - should redirect")
void redirect_shouldReturnFound() throws Exception {

    // Arrange
    String shortCode = "Ab12Cd";
    String originalUrl = "https://www.google.com";

    when(service.getOriginalUrl(shortCode))
            .thenReturn(originalUrl);

    // Act & Assert
    mockMvc.perform(
                    get("/{shortCode}", shortCode)
            )
            .andExpect(status().isFound())
            .andExpect(
                    header().string(
                            "Location",
                            originalUrl
                    )
            );

    verify(service, times(1))
            .getOriginalUrl(shortCode);
}

/**
 * Tests redirection when the short code does not exist.
 */
@Test
@DisplayName("GET /{shortCode} - should return 404")
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
                    get("/{shortCode}", shortCode)
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

    verify(service, times(1))
            .getOriginalUrl(shortCode);
}

/**
 * Tests redirection when the short URL has expired.
 */
@Test
@DisplayName("GET /{shortCode} - should return 410 when expired")
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
                    get("/{shortCode}", shortCode)
            )
            .andExpect(status().isGone())
            .andExpect(
                    jsonPath("$.status")
                            .value(410)
            )
            .andExpect(
                    jsonPath("$.error")
                            .value("URL Expired")
            )
            .andExpect(
                    jsonPath("$.message")
                            .value(
                                    "Short URL 'Ab12Cd' has expired"
                            )
            );

    verify(service, times(1))
            .getOriginalUrl(shortCode);
}

}
