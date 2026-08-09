package com.sandeep.urlshortener.service.impl;

import com.sandeep.urlshortener.dto.request.CreateShortUrlRequest;
import com.sandeep.urlshortener.dto.response.CreateShortUrlResponse;
import com.sandeep.urlshortener.dto.response.UrlStatsResponse;
import com.sandeep.urlshortener.entity.Url;
import com.sandeep.urlshortener.exception.ResourceNotFoundException;
import com.sandeep.urlshortener.exception.UrlExpiredException;
import com.sandeep.urlshortener.repository.UrlRepository;
import com.sandeep.urlshortener.service.UrlService;
import com.sandeep.urlshortener.util.ShortCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementation of {@link UrlService}.
 *
 * <p>Handles the core business logic for creating, redirecting,
 * retrieving statistics for, and deleting shortened URLs.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    /**
     * Repository responsible for URL database operations.
     */
    private final UrlRepository repository;

    /**
     * Utility responsible for generating short codes.
     */
    private final ShortCodeGenerator generator;

    /**
     * Creates a new shortened URL.
     *
     * <p>A unique short code is generated and checked against the database
     * before the URL is persisted.</p>
     *
     * @param request request containing the original URL and optional expiration time
     * @return response containing details of the newly created short URL
     */
    @Override
    public CreateShortUrlResponse createShortUrl(
            CreateShortUrlRequest request) {

        log.info(
                "Creating short URL for: {}",
                request.getOriginalUrl()
        );

        String code;

        /*
         * Generate a short code until a unique code is found.
         *
         * The database check prevents two URLs from being assigned
         * the same short code.
         */
        do {
            code = generator.generate();
        } while (repository.existsByShortCode(code));

        /*
         * Build the URL entity.
         *
         * createdAt and clickCount are initialized automatically
         * by the Url entity.
         */
        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(code)
                .expiresAt(request.getExpiresAt())
                .build();

        /*
         * Persist the newly created URL.
         */
        Url savedUrl = repository.save(url);

        log.info(
                "Short URL created successfully. Code: {}",
                savedUrl.getShortCode()
        );

        /*
         * Convert the entity into the response DTO.
         */
        return CreateShortUrlResponse.builder()
                .shortCode(savedUrl.getShortCode())
                .originalUrl(savedUrl.getOriginalUrl())
                .shortUrl(
                        "http://localhost:8080/"
                                + savedUrl.getShortCode()
                )
                .createdAt(savedUrl.getCreatedAt())
                .expiresAt(savedUrl.getExpiresAt())
                .clickCount(savedUrl.getClickCount())
                .build();
    }

    /**
     * Retrieves the original URL associated with a short code.
     *
     * <p>The method performs the following operations:</p>
     * <ul>
     *     <li>Finds the short URL in the database.</li>
     *     <li>Throws 404-related exception when it does not exist.</li>
     *     <li>Checks whether the URL has expired.</li>
     *     <li>Throws 410-related exception when expired.</li>
     *     <li>Increments the click count.</li>
     *     <li>Returns the original URL.</li>
     * </ul>
     *
     * @param shortCode unique short code
     * @return original URL
     * @throws ResourceNotFoundException if the short code does not exist
     * @throws UrlExpiredException if the short URL has expired
     */
    @Override
    public String getOriginalUrl(String shortCode) {

        log.info(
                "Redirect request received for short code: {}",
                shortCode
        );

        /*
         * Find the URL by its short code.
         *
         * A missing URL is a normal client/resource condition,
         * therefore WARN is more appropriate than ERROR.
         */
        Url url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> {

                    log.warn(
                            "Short URL not found: {}",
                            shortCode
                    );

                    return new ResourceNotFoundException(
                            "Short URL '" + shortCode + "' not found"
                    );
                });

        /*
         * Check whether the URL has expired.
         *
         * A null expiration date means the URL never expires.
         */
        if (url.getExpiresAt() != null
                && url.getExpiresAt().isBefore(LocalDateTime.now())) {

            log.warn(
                    "Expired URL accessed: {}",
                    shortCode
            );

            throw new UrlExpiredException(
                    "Short URL '" + shortCode + "' has expired"
            );
        }

        /*
         * Increment the click count.
         *
         * The entity normally initializes clickCount to 0,
         * but the null check makes the service more defensive.
         */
        long currentClickCount =
                url.getClickCount() != null
                        ? url.getClickCount()
                        : 0L;

        url.setClickCount(currentClickCount + 1);

        /*
         * Persist the updated click count.
         */
        repository.save(url);

        log.info(
                "Redirect successful. Code={}, Click count={}",
                shortCode,
                url.getClickCount()
        );

        return url.getOriginalUrl();
    }

    /**
     * Retrieves statistics for a shortened URL.
     *
     * @param shortCode unique short code
     * @return statistics associated with the short URL
     * @throws ResourceNotFoundException if the short code does not exist
     */
    @Override
    public UrlStatsResponse getStatistics(String shortCode) {

        log.info(
                "Fetching statistics for short code: {}",
                shortCode
        );

        /*
         * Retrieve the URL from the database.
         */
        Url url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> {

                    log.warn(
                            "Statistics requested for unknown short code: {}",
                            shortCode
                    );

                    return new ResourceNotFoundException(
                            "Short URL '" + shortCode + "' not found"
                    );
                });

        /*
         * Convert the entity into the statistics response DTO.
         */
        return UrlStatsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .build();
    }

    /**
     * Deletes an existing shortened URL.
     *
     * @param shortCode unique short code
     * @throws ResourceNotFoundException if the short code does not exist
     */
    @Override
    public void deleteShortUrl(String shortCode) {

        log.info(
                "Deleting short URL: {}",
                shortCode
        );

        /*
         * Find the URL before deleting it.
         *
         * This allows the service to return a meaningful 404
         * when the requested short code does not exist.
         */
        Url url = repository.findByShortCode(shortCode)
                .orElseThrow(() -> {

                    log.warn(
                            "Delete failed. Short URL not found: {}",
                            shortCode
                    );

                    return new ResourceNotFoundException(
                            "Short URL '" + shortCode + "' not found"
                    );
                });

        /*
         * Delete the URL entity.
         */
        repository.delete(url);

        log.info(
                "Short URL deleted successfully: {}",
                shortCode
        );
    }
}