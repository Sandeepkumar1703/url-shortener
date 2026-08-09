package com.sandeep.urlshortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final String CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String generate() {

        StringBuilder shortCode = new StringBuilder();

        for (int i = 0; i < LENGTH; i++) {
            shortCode.append(
                    CHARACTERS.charAt(random.nextInt(CHARACTERS.length()))
            );
        }

        return shortCode.toString();
    }
}