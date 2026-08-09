package com.sandeep.urlshortener.config;

import com.sandeep.urlshortener.interceptor.RateLimitInterceptor;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(
            InterceptorRegistry registry) {

        registry
                .addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/api/v1/urls",
                        "/api/v1/urls/**",
                        "/**"
                );
    }
}