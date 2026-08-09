package com.sandeep.urlshortener.service.impl;

import com.sandeep.urlshortener.service.RateLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean isAllowed(String key, int limit, long windowSeconds) {

        Long count = redisTemplate.opsForValue().increment(key);

        log.info("RateLimit Key = {}", key);
        log.info("Current Count = {}", count);
        log.info("Limit = {}", limit);

        if (count == null) {
            return false;
        }

        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        return count <= limit;
    }

    @Override
    public long getRemainingRequests(String key, int limit) {

        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return limit;
        }

        try {
            long count = Long.parseLong(value);
            return Math.max(0, limit - count);
        } catch (NumberFormatException ex) {
            log.warn("Invalid rate limit value for key {} : {}", key, value);
            return 0;
        }
    }

    @Override
    public long getRetryAfterSeconds(String key) {

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        if (ttl == null || ttl < 0) {
            return 0;
        }

        return ttl;
    }
}