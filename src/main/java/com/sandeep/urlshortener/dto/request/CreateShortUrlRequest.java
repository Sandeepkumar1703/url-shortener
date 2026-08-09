package com.sandeep.urlshortener.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShortUrlRequest {

    @NotBlank(message = "Original URL is required")
    @URL(message = "Please provide a valid URL")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String originalUrl;

    @Future(message = "Expiration date must be in the future")
    private LocalDateTime expiresAt;
}