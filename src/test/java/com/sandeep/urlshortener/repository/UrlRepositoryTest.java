package com.sandeep.urlshortener.repository;

import com.sandeep.urlshortener.entity.Url;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UrlRepository}.
 *
 * Uses a real PostgreSQL database running inside Testcontainers.
 *
 * This verifies that the JPA repository works correctly against
 * PostgreSQL rather than using an in-memory database.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class UrlRepositoryTest {

    /**
     * PostgreSQL container used for repository integration tests.
     *
     * The container is started once for this test class and stopped
     * after the test class finishes.
     */
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("url_shortener_test")
                    .withUsername("test")
                    .withPassword("test");

    /**
     * Provides the Testcontainers PostgreSQL connection details
     * to Spring Boot.
     */
    @DynamicPropertySource
    static void configureDatabase(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );

        /*
         * Flyway is disabled because this test focuses specifically
         * on JPA repository behavior.
         */
        registry.add(
                "spring.flyway.enabled",
                () -> false
        );

        /*
         * Hibernate creates the test schema automatically.
         */
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );
    }

    @Autowired
    private UrlRepository repository;

    @Test
    @DisplayName("save - should persist URL successfully")
    void save_shouldPersistUrl() {

        // Arrange
        Url url = Url.builder()
                .shortCode("Ab12Cd")
                .originalUrl("https://www.google.com")
                .build();

        // Act
        Url savedUrl = repository.save(url);

        // Assert
        assertThat(savedUrl.getId())
                .isNotNull();

        assertThat(savedUrl.getShortCode())
                .isEqualTo("Ab12Cd");

        assertThat(savedUrl.getOriginalUrl())
                .isEqualTo("https://www.google.com");

        assertThat(savedUrl.getClickCount())
                .isEqualTo(0L);

        assertThat(savedUrl.getCreatedAt())
                .isNotNull();
    }

    @Test
    @DisplayName("findByShortCode - should return URL")
    void findByShortCode_shouldReturnUrl() {

        // Arrange
        Url url = Url.builder()
                .shortCode("Ab12Cd")
                .originalUrl("https://www.google.com")
                .build();

        repository.save(url);

        // Act
        Optional<Url> result =
                repository.findByShortCode("Ab12Cd");

        // Assert
        assertThat(result)
                .isPresent();

        assertThat(result.get().getShortCode())
                .isEqualTo("Ab12Cd");

        assertThat(result.get().getOriginalUrl())
                .isEqualTo("https://www.google.com");
    }

    @Test
    @DisplayName("findByShortCode - should return empty when not found")
    void findByShortCode_shouldReturnEmptyWhenNotFound() {

        // Act
        Optional<Url> result =
                repository.findByShortCode("Unknown");

        // Assert
        assertThat(result)
                .isEmpty();
    }

    @Test
    @DisplayName("existsByShortCode - should return true")
    void existsByShortCode_shouldReturnTrue() {

        // Arrange
        Url url = Url.builder()
                .shortCode("Ab12Cd")
                .originalUrl("https://www.google.com")
                .build();

        repository.save(url);

        // Act
        boolean exists =
                repository.existsByShortCode("Ab12Cd");

        // Assert
        assertThat(exists)
                .isTrue();
    }

    @Test
    @DisplayName("existsByShortCode - should return false")
    void existsByShortCode_shouldReturnFalse() {

        // Act
        boolean exists =
                repository.existsByShortCode("Unknown");

        // Assert
        assertThat(exists)
                .isFalse();
    }

    @Test
    @DisplayName("deleteByShortCode - should delete URL")
    void deleteByShortCode_shouldDeleteUrl() {

        // Arrange
        Url url = Url.builder()
                .shortCode("Ab12Cd")
                .originalUrl("https://www.google.com")
                .build();

        repository.save(url);

        // Act
        repository.deleteByShortCode("Ab12Cd");

        // Assert
        assertThat(
                repository.findByShortCode("Ab12Cd")
        ).isEmpty();
    }

    @Test
    @DisplayName("save - should persist expiration date")
    void save_shouldPersistExpirationDate() {

        // Arrange
        LocalDateTime expiration =
                LocalDateTime.now().plusDays(7);

        Url url = Url.builder()
                .shortCode("Expire1")
                .originalUrl("https://example.com")
                .expiresAt(expiration)
                .build();

        // Act
        Url savedUrl =
                repository.save(url);

        // Assert
        assertThat(savedUrl.getExpiresAt())
                .isEqualTo(expiration);
    }

    @Test
    @DisplayName("save - should persist click count")
    void save_shouldPersistClickCount() {

        // Arrange
        Url url = Url.builder()
                .shortCode("Clicks")
                .originalUrl("https://example.com")
                .clickCount(10L)
                .build();

        // Act
        Url savedUrl =
                repository.save(url);

        // Assert
        assertThat(savedUrl.getClickCount())
                .isEqualTo(10L);
    }
}