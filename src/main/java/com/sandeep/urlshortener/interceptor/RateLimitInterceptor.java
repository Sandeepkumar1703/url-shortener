package com.sandeep.urlshortener.interceptor;

import com.sandeep.urlshortener.service.RateLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    private static final int CREATE_LIMIT = 10;
    private static final int REDIRECT_LIMIT = 60;
    private static final int STATS_LIMIT = 30;
    private static final int DELETE_LIMIT = 10;

    private static final long WINDOW_SECONDS = 60;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for documentation and actuator endpoints
        if (isExcludedPath(path)) {
            return true;
        }

        RateLimitRule rule = getRateLimitRule(method, path);

        // Endpoint is not rate limited
        if (rule == null) {
            return true;
        }

        String clientIp = getClientIp(request);

        String redisKey = "rate_limit:" + rule.name() + ":" + clientIp;

        boolean allowed = rateLimitService.isAllowed(
                redisKey,
                rule.limit(),
                WINDOW_SECONDS
        );

        long remaining = rateLimitService.getRemainingRequests(
                redisKey,
                rule.limit()
        );

        response.setHeader(
                "X-RateLimit-Limit",
                String.valueOf(rule.limit())
        );

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(remaining)
        );

        if (!allowed) {

            long retryAfter =
                    rateLimitService.getRetryAfterSeconds(redisKey);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

            response.setHeader(
                    "Retry-After",
                    String.valueOf(retryAfter)
            );

            response.setContentType("application/json");

            String body = """
                    {
                      "status": 429,
                      "error": "Too Many Requests",
                      "message": "Rate limit exceeded. Please try again later.",
                      "retryAfterSeconds": %d
                    }
                    """.formatted(retryAfter);

            response.getWriter().write(body);

            log.warn(
                    "Rate limit exceeded. IP={}, Method={}, Path={}, Limit={}",
                    clientIp,
                    method,
                    path,
                    rule.limit()
            );

            return false;
        }

        return true;
    }

    private RateLimitRule getRateLimitRule(
            String method,
            String path) {

        // POST /api/v1/urls
        if ("POST".equalsIgnoreCase(method)
                && "/api/v1/urls".equals(path)) {

            return new RateLimitRule(
                    "create",
                    CREATE_LIMIT
            );
        }

        // GET /api/v1/urls/{shortCode}/stats
        if ("GET".equalsIgnoreCase(method)
                && path.matches("/api/v1/urls/[^/]+/stats")) {

            return new RateLimitRule(
                    "stats",
                    STATS_LIMIT
            );
        }

        // DELETE /api/v1/urls/{shortCode}
        if ("DELETE".equalsIgnoreCase(method)
                && path.matches("/api/v1/urls/[^/]+")) {

            return new RateLimitRule(
                    "delete",
                    DELETE_LIMIT
            );
        }

        // GET /{shortCode}
        if ("GET".equalsIgnoreCase(method)
                && path.matches("/[^/]+")) {

            return new RateLimitRule(
                    "redirect",
                    REDIRECT_LIMIT
            );
        }

        return null;
    }

    private boolean isExcludedPath(String path) {

        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.equals("/")
                || path.startsWith("/favicon");
    }

    private String getClientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private record RateLimitRule(
            String name,
            int limit
    ) {
    }
}