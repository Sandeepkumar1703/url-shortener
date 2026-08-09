package com.sandeep.urlshortener.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    @Bean
    public ObjectMapper redisObjectMapper() {

        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {

        log.info("=================================================");
        log.info("Redis Connection Factory: {}",
                connectionFactory.getClass().getName());

        try {
            RedisConnection connection =
                    connectionFactory.getConnection();

            log.info("Redis PING response: {}",
                    connection.ping());

            log.info("Redis connection factory details: {}",
                    connectionFactory);

            connection.close();

        } catch (Exception e) {

            log.error("Unable to connect to Redis", e);
        }

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(
                        redisObjectMapper
                );

        RedisCacheConfiguration configuration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .disableCachingNullValues()
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(serializer)
                        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}