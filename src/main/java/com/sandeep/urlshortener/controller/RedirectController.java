package com.sandeep.urlshortener.controller;

import com.sandeep.urlshortener.service.UrlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "URL Redirect",
        description = "Redirects short URLs to their original URLs"
)
public class RedirectController {

    private final UrlService service;

    /**
     * Redirects a short URL to the original URL.
     *
     * <p>
     * Example:
     *
     * GET http://localhost:8080/xV2g82
     *
     * <p>
     * The response contains HTTP 302 Found with the original
     * URL in the Location header.
     */
    @Operation(
            summary = "Redirect Short URL",
            description = "Redirects a short code to its original URL"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "Redirects to the original URL"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Short URL not found"
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Short URL has expired"
            )
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(
                    description = "Unique short URL code",
                    example = "xV2g82"
            )
            @PathVariable String shortCode) {

        String originalUrl = service.getOriginalUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}
