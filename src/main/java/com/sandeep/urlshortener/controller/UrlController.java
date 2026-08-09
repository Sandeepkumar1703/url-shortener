package com.sandeep.urlshortener.controller;

import com.sandeep.urlshortener.dto.request.CreateShortUrlRequest;
import com.sandeep.urlshortener.dto.response.CreateShortUrlResponse;
import com.sandeep.urlshortener.dto.response.UrlStatsResponse;
import com.sandeep.urlshortener.service.UrlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST controller for creating and managing shortened URLs.
 *
 * <p>Provides endpoints to:</p>
 * <ul>
 *     <li>Create a short URL</li>
 *     <li>Redirect to the original URL</li>
 *     <li>Retrieve URL statistics</li>
 *     <li>Delete a short URL</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
@Tag(
        name = "URL Shortener",
        description = "APIs for creating, redirecting, managing, and monitoring short URLs"
)
public class UrlController {

    /**
     * Service responsible for URL shortening business logic.
     */
    private final UrlService service;

    /**
     * Creates a new shortened URL.
     *
     * <p>The request URL is validated before being passed
     * to the service layer.</p>
     *
     * @param request request containing the original URL and optional expiration date
     * @return newly created short URL details
     */
    @Operation(
            summary = "Create Short URL",
            description = "Creates a unique short URL for the provided original URL"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateShortUrlResponse create(
            @Valid @RequestBody CreateShortUrlRequest request) {

        return service.createShortUrl(request);
    }

    /**
     * Redirects the client to the original URL.
     *
     * <p>The service layer verifies that the short code exists
     * and that the URL has not expired. A successful request
     * returns HTTP 302 Found with the original URL in the
     * Location header.</p>
     *
     * @param shortCode unique short URL code
     * @return HTTP 302 response containing the redirect location
     */
    @Operation(
            summary = "Redirect to Original URL",
            description = "Redirects the client to the original URL associated with the short code"
    )
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(
                    description = "Unique short URL code",
                    example = "Ab12Cd"
            )
            @PathVariable String shortCode) {

        String originalUrl = service.getOriginalUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    /**
     * Retrieves statistics for a short URL.
     *
     * <p>Returns information such as the original URL,
     * creation time, expiration time, and click count.</p>
     *
     * @param shortCode unique short URL code
     * @return statistics for the requested short URL
     */
    @Operation(
            summary = "Get URL Statistics",
            description = "Returns statistics and metadata for a short URL"
    )
    @GetMapping("/{shortCode}/stats")
    public UrlStatsResponse getStatistics(
            @Parameter(
                    description = "Unique short URL code",
                    example = "Ab12Cd"
            )
            @PathVariable String shortCode) {

        return service.getStatistics(shortCode);
    }

    /**
     * Deletes an existing shortened URL.
     *
     * <p>Once deleted, the short code can no longer be used
     * for redirection or statistics retrieval.</p>
     *
     * @param shortCode unique short URL code
     */
    @Operation(
            summary = "Delete Short URL",
            description = "Permanently deletes the short URL associated with the provided short code"
    )
    @DeleteMapping("/{shortCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(
                    description = "Unique short URL code",
                    example = "Ab12Cd"
            )
            @PathVariable String shortCode) {

        service.deleteShortUrl(shortCode);
    }
}