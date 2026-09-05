package com.weathergpt.weather.geocoding;

import com.weathergpt.exception.WeatherProviderException;
import com.weathergpt.weather.GeocodingProvider;
import com.weathergpt.weather.model.GeoLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Geocoding provider backed by the free Open-Meteo Geocoding API
 * (https://open-meteo.com — no API key required).
 * Resolves human-readable place names to normalized coordinates.
 */
@Slf4j
@Service
public class OpenMeteoGeocodingProvider implements GeocodingProvider {

    private final RestTemplate restTemplate;

    @Value("${geocoding.api.base-url:https://geocoding-api.open-meteo.com/v1}")
    private String baseUrl;

    public OpenMeteoGeocodingProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<GeoLocation> resolve(String query) {
        String url = baseUrl + "/search?name=" + encode(query) + "&count=1&language=en&format=json";
        try {
            Map<String, Object> raw = restTemplate.getForObject(url, Map.class);
            if (raw == null) {
                return Optional.empty();
            }
            List<Map<String, Object>> results = (List<Map<String, Object>>) raw.get("results");
            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }
            Map<String, Object> first = results.get(0);
            return Optional.of(GeoLocation.builder()
                    .name((String) first.get("name"))
                    .latitude(asDouble(first.get("latitude")))
                    .longitude(asDouble(first.get("longitude")))
                    .admin1((String) first.get("admin1"))
                    .country((String) first.get("country"))
                    .timezone((String) first.get("timezone"))
                    .build());
        } catch (RestClientException e) {
            log.error("Open-Meteo geocoding request failed for '{}': {}", query, e.getMessage());
            throw new WeatherProviderException(
                    "Location service is temporarily unavailable. Please try again later.", e);
        }
    }

    private static String encode(String query) {
        return URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    private static Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
