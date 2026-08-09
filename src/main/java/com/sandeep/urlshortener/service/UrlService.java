package com.sandeep.urlshortener.service;

import com.sandeep.urlshortener.dto.request.CreateShortUrlRequest;
import com.sandeep.urlshortener.dto.response.CreateShortUrlResponse;
import com.sandeep.urlshortener.dto.response.UrlStatsResponse;

public interface UrlService {

    CreateShortUrlResponse createShortUrl(CreateShortUrlRequest request);

    String getOriginalUrl(String shortCode);

    UrlStatsResponse getStatistics(String shortCode);

    void deleteShortUrl(String shortCode);
}