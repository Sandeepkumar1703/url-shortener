package com.sandeep.urlshortener.exception;

/**
 * Exception thrown when a short URL has expired.
 *
 * Results in an HTTP 410 Gone response.
 */
public class UrlExpiredException extends RuntimeException {

    /**
     * Creates a new UrlExpiredException with the specified message.
     *
     * @param message descriptive error message
     */
    public UrlExpiredException(String message) {
        super(message);
    }
}