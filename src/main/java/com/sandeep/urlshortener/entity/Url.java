package com.sandeep.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ================================================================
 * URL Entity
 * ================================================================
 *
 * Represents a shortened URL stored in the database.
 *
 * Responsibilities:
 * -----------------
 * • Stores the original URL.
 * • Stores the generated short code.
 * • Tracks creation and expiration timestamps.
 * • Tracks redirect statistics.
 * • Supports optimistic locking.
 * * Supports soft activation/deactivation.
 *
 * This entity is mapped to the 'urls' table.
 */
@Entity
@Table(
        name = "urls",
        indexes = {
                @Index(name = "idx_url_short_code", columnList = "shortCode"),
                @Index(name = "idx_url_created_at", columnList = "createdAt"),
                @Index(name = "idx_url_expires_at", columnList = "expiresAt")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Url {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique short code used for redirection.
     *
     * Example:
     * abc123
     */
    @Column(nullable = false, unique = true, length = 10)
    private String shortCode;

    /**
     * Original destination URL.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    /**
     * Timestamp when the short URL was created.
     *
     * Never updated.
     */
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Timestamp of the most recent update.
     *
     * Automatically updated on every entity modification.
     */
    private LocalDateTime updatedAt;

    /**
     * Expiration timestamp.
     *
     * Null means the URL never expires.
     */
    private LocalDateTime expiresAt;

    /**
     * Timestamp of the most recent redirect.
     *
     * Updated whenever someone accesses the short URL.
     */
    private LocalDateTime lastAccessedAt;

    /**
     * Total number of successful redirects.
     */
    @Builder.Default
    @Column(nullable = false)
    private Long clickCount = 0L;

    /**
     * Indicates whether the short URL is active.
     *
     * Future use:
     * - Soft delete
     * - Admin disable
     * - Temporary suspension
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * Optimistic locking column.
     *
     * Prevents concurrent updates from overwriting each other.
     */
    @Version
    private Long version;

    /**
     * Executed before the entity is inserted.
     */
    @PrePersist
    public void prePersist() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (clickCount == null) {
            clickCount = 0L;
        }

        if (isActive == null) {
            isActive = true;
        }
    }

    /**
     * Executed before the entity is updated.
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}