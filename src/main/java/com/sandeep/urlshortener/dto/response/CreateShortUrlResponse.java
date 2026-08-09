package com.sandeep.urlshortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response returned after successfully creating a shortened URL.
 *
 * <p>Contains the generated short code, the original URL,
 * the complete short URL, creation and expiration timestamps,
 * and the current number of clicks.</p>
 */
@Data
@Builder
@Schema(
        name = "CreateShortUrlResponse",
        description = "Response containing details of a newly created short URL"
)
public class CreateShortUrlResponse {

    /**
     * Unique short code generated for the original URL.
     *
     * <p>Example: Ab12Cd</p>
     */
    @Schema(
            description = "Unique short code generated for the original URL",
            example = "Ab12Cd"
    )
    private String shortCode;

    /**
     * The original URL that was shortened.
     *
     * <p>Example: https://www.google.com</p>
     */
    @Schema(
            description = "Original URL provided by the client",
            example = "https://www.google.com"
    )
    private String originalUrl;

    /**
     * Complete shortened URL that can be shared with users.
     *
     * <p>Example: http://localhost:8080/Ab12Cd</p>
     */
    @Schema(
            description = "Complete shortened URL",
            example = "http://localhost:8080/Ab12Cd"
    )
    private String shortUrl;

    /**
     * Date and time when the short URL was created.
     */
    @Schema(
            description = "Date and time when the short URL was created",
            example = "2026-08-09T11:30:00"
    )
    private LocalDateTime createdAt;

    /**
     * Date and time when the short URL will expire.
     *
     * <p>This field is null when the URL does not have
     * an expiration time.</p>
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
            example = "0"
    )
    private Long clickCount;
}