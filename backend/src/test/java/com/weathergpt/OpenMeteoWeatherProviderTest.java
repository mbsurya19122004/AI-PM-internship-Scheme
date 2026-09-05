package com.weathergpt;

import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.exception.WeatherProviderException;
import com.weathergpt.weather.model.GeoLocation;
import com.weathergpt.weather.provider.OpenMeteoWeatherProvider;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_provider_weather",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ="
})
class OpenMeteoWeatherProviderTest {

    @Autowired
    private OpenMeteoWeatherProvider provider;

    @MockBean
    private RestTemplate restTemplate;

    private static final GeoLocation DELHI = GeoLocation.builder()
            .name("Delhi")
            .latitude(28.65195)
            .longitude(77.23149)
            .admin1("National Capital Territory of Delhi")
            .country("India")
            .timezone("Asia/Kolkata")
            .build();

    @BeforeEach
    void stubTemplate() {
        // Ensure no test accidentally performs a real HTTP call
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new AssertionError("Unexpected unmocked HTTP call"));
    }

    @Test
    @DisplayName("Current weather raw payload is normalized into the public DTO")
    void currentWeatherNormalization() {
        Map<String, Object> current = Map.of(
                "time", "2026-09-04T21:30",
                "temperature_2m", 27.9,
                "apparent_temperature", 30.1,
                "relative_humidity_2m", 62,
                "wind_speed_10m", 11.2,
                "wind_direction_10m", 245,
                "weather_code", 2,
                "pressure_msl", 1004.3,
                "visibility", 6542,
                "is_day", 0
        );
        Map<String, Object> raw = Map.of("timezone", "Asia/Kolkata", "current", current);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(raw);

        CurrentWeatherResponse response = provider.getCurrentWeather(DELHI);

        assertEquals(27.9, response.getTemperature());
        assertEquals(30.1, response.getApparentTemperature());
        assertEquals(62, response.getHumidity());
        assertEquals(11.2, response.getWindSpeed());
        assertEquals(245, response.getWindDirection());
        assertEquals(2, response.getWeatherCode());
        assertEquals("Partly cloudy", response.getWeatherDescription());
        assertEquals(1004.3, response.getPressure());
        assertEquals(6542, response.getVisibility());
        assertEquals(Boolean.FALSE, response.getIsDay());
        assertEquals("2026-09-04T21:30", response.getObservedAt());
        assertEquals("Asia/Kolkata", response.getTimezone());
        assertEquals("Open-Meteo", response.getProvider());
        assertEquals("Delhi", response.getLocation().getName());
        assertEquals(28.65195, response.getLocation().getLatitude());
    }

    @Test
    @DisplayName("Forecast raw payload is normalized into the public DTO")
    void forecastNormalization() {
        Map<String, Object> daily = Map.of(
                "time", List.of("2026-09-04", "2026-09-05"),
                "weather_code", List.of(2, 63),
                "temperature_2m_max", List.of(34.2, 33.1),
                "temperature_2m_min", List.of(26.1, 25.4),
                "precipitation_probability_max", List.of(10, 80),
                "precipitation_sum", List.of(0.0, 12.5),
                "wind_speed_10m_max", List.of(18.4, 22.0),
                "relative_humidity_2m_max", List.of(78, 90)
        );
        Map<String, Object> raw = Map.of("timezone", "Asia/Kolkata", "daily", daily);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(raw);

        ForecastResponse response = provider.getForecast(DELHI, 2);

        assertEquals(2, response.getDays().size());
        assertEquals("2026-09-04", response.getDays().get(0).getDate());
        assertEquals(34.2, response.getDays().get(0).getTempMax());
        assertEquals(26.1, response.getDays().get(0).getTempMin());
        assertEquals(10, response.getDays().get(0).getPrecipitationProbabilityMax());
        assertEquals(0.0, response.getDays().get(0).getPrecipitationSum());
        assertEquals(18.4, response.getDays().get(0).getWindSpeedMax());
        assertEquals("Partly cloudy", response.getDays().get(0).getWeatherDescription());
        assertEquals(63, response.getDays().get(1).getWeatherCode());
        assertEquals("Moderate rain", response.getDays().get(1).getWeatherDescription());
        assertEquals("Open-Meteo", response.getProvider());
    }

    @Test
    @DisplayName("HTTP failure is converted to WeatherProviderException")
    void httpFailureConvertsToWeatherProviderException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        assertThrows(WeatherProviderException.class, () -> provider.getCurrentWeather(DELHI));
        assertThrows(WeatherProviderException.class, () -> provider.getForecast(DELHI, 7));
    }
}