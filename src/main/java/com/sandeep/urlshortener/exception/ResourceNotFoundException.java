package com.sandeep.urlshortener.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 *
 * Results in an HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates a new ResourceNotFoundException with the specified message.
     *
     * @param message descriptive error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}