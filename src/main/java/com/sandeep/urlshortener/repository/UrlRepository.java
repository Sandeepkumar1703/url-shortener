package com.sandeep.urlshortener.repository;

import com.sandeep.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    boolean existsByShortCode(String shortCode);

    Optional<Url> findByShortCode(String shortCode);

    void deleteByShortCode(String shortCode);
}