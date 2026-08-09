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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for creating and managing shortened URLs.
 *
 * <p>Handles:</p>
 * <ul>
 *     <li>Create short URLs</li>
 *     <li>Retrieve URL statistics</li>
 *     <li>Delete short URLs</li>
 * </ul>
 *
 * <p>Redirect functionality is intentionally handled
 * by {@link RedirectController} because redirects use
 * the root path /{shortCode}.</p>
 */
@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
@Tag(
        name = "URL Shortener",
        description = "APIs for creating, managing, and monitoring short URLs"
)
public class UrlController {

    private final UrlService service;

    /**
     * Creates a new short URL.
     *
     * @param request request containing the original URL
     *                and optional expiration date
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
     * Retrieves statistics for a short URL.
     *
     * @param shortCode unique short URL code
     * @return URL statistics
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
     * Deletes a short URL.
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