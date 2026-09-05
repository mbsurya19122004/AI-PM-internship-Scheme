package com.weathergpt;

import com.weathergpt.exception.WeatherProviderException;
import com.weathergpt.weather.geocoding.OpenMeteoGeocodingProvider;
import com.weathergpt.weather.model.GeoLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_provider_geo",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ="
})
class OpenMeteoGeocodingProviderTest {

    @Autowired
    private OpenMeteoGeocodingProvider provider;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void stubTemplate() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new AssertionError("Unexpected unmocked HTTP call"));
    }

    @Test
    @DisplayName("Valid place name resolves to normalized coordinates")
    void resolvesValidLocation() {
        Map<String, Object> result = Map.of(
                "name", "Delhi",
                "latitude", 28.65195,
                "longitude", 77.23149,
                "admin1", "National Capital Territory of Delhi",
                "country", "India",
                "timezone", "Asia/Kolkata"
        );
        Map<String, Object> raw = Map.of("results", List.of(result));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(raw);

        Optional<GeoLocation> resolved = provider.resolve("Delhi");

        assertTrue(resolved.isPresent());
        GeoLocation location = resolved.get();
        assertEquals("Delhi", location.getName());
        assertEquals(28.65195, location.getLatitude());
        assertEquals(77.23149, location.getLongitude());
        assertEquals("India", location.getCountry());
        assertEquals("Asia/Kolkata", location.getTimezone());
    }

    @Test
    @DisplayName("Unknown place returns empty result")
    void unknownPlaceReturnsEmpty() {
        Map<String, Object> raw = Map.of("results", List.of());
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(raw);

        assertTrue(provider.resolve("InvalidFakeLocationXYZ").isEmpty());
    }

    @Test
    @DisplayName("Empty provider response returns empty result")
    void emptyResponseReturnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        assertTrue(provider.resolve("Nowhere").isEmpty());
    }

    @Test
    @DisplayName("Geocoding HTTP failure is converted to WeatherProviderException")
    void httpFailureConvertsToWeatherProviderException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        assertThrows(WeatherProviderException.class, () -> provider.resolve("Delhi"));
    }
}