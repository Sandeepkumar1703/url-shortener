package com.sandeep.urlshortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response containing statistics and metadata for a shortened URL.
 *
 * <p>This response provides information about the original URL,
 * its creation and expiration times, and the number of times
 * the short URL has been accessed.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "UrlStatsResponse",
        description = "Statistics and metadata associated with a short URL"
)
public class UrlStatsResponse {

    /**
     * Unique short code associated with the URL.
     *
     * <p>Example: Ab12Cd</p>
     */
    @Schema(
            description = "Unique short code associated with the URL",
            example = "Ab12Cd"
    )
    private String shortCode;

    /**
     * Original URL associated with the short code.
     *
     * <p>Example: https://www.google.com</p>
     */
    @Schema(
            description = "Original URL associated with the short code",
            example = "https://www.google.com"
    )
    private String originalUrl;

    /**
     * Date and time when the short URL was created.
     */
    @Schema(
            description = "Date and time when the short URL was created",
            example = "2026-08-09T11:30:00"
    )
    private LocalDateTime createdAt;

    /**
     * Date and time when the short URL expires.
     *
     * <p>This field is null when no expiration date
     * was configured.</p>
     */
    @Schema(
            description = "Date and time when the short URL expires",
            example = "2026-08-10T11:30:00",
            nullable = true
    )
    private LocalDateTime expiresAt;

    /**
     * Number of times the short URL has been accessed.
     */
    @Schema(
            description = "Number of times the short URL has been accessed",
            example = "42"
    )
    private Long clickCount;
}