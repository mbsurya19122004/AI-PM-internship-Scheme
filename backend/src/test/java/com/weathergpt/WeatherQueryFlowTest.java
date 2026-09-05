package com.weathergpt;

import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastDay;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.exception.WeatherProviderException;
import com.weathergpt.weather.GeocodingProvider;
import com.weathergpt.weather.WeatherProvider;
import com.weathergpt.weather.model.GeoLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end natural-language flow: real interpreter + orchestration + generator,
 * with the external geocoding/weather providers mocked so no live API is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_chat",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ="
})
class WeatherQueryFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeocodingProvider geocodingProvider;

    @MockBean
    private WeatherProvider weatherProvider;

    private static final GeoLocation DELHI = geo("Delhi", 28.65, 77.23);
    private static final GeoLocation MUMBAI = geo("Mumbai", 19.08, 72.88);
    private static final GeoLocation BENGALURU = geo("Bengaluru", 12.97, 77.59);
    private static final GeoLocation KOLKATA = geo("Kolkata", 22.57, 88.36);

    private CurrentWeatherResponse currentFixture() {
        return CurrentWeatherResponse.builder()
                .temperature(28.0)
                .apparentTemperature(30.5)
                .humidity(60)
                .windSpeed(12.0)
                .windDirection(180)
                .weatherCode(2)
                .weatherDescription("Partly cloudy")
                .observedAt("2026-09-04T21:00")
                .timezone("Asia/Kolkata")
                .provider("Open-Meteo")
                .build();
    }

    private ForecastResponse forecastFixture() {
        ForecastDay today = ForecastDay.builder()
                .date("2026-09-04").weatherCode(2).weatherDescription("Partly cloudy")
                .tempMax(32.0).tempMin(24.0)
                .precipitationProbabilityMax(10).precipitationSum(0.0)
                .windSpeedMax(12.0).humidityMax(70).build();
        ForecastDay tomorrow = ForecastDay.builder()
                .date("2026-09-05").weatherCode(61).weatherDescription("Slight rain")
                .tempMax(30.0).tempMin(24.0)
                .precipitationProbabilityMax(75).precipitationSum(3.2)
                .windSpeedMax(18.0).humidityMax(85).build();
        return ForecastResponse.builder()
                .timezone("Asia/Kolkata")
                .provider("Open-Meteo")
                .days(List.of(today, tomorrow))
                .build();
    }

    private static GeoLocation geo(String name, double lat, double lon) {
        return GeoLocation.builder().name(name).latitude(lat).longitude(lon)
                .country("India").timezone("Asia/Kolkata").build();
    }

    @BeforeEach
    void stubProviders() {
        // Default: any unknown location does not resolve
        when(geocodingProvider.resolve(anyString())).thenReturn(Optional.empty());
        when(geocodingProvider.resolve("Delhi")).thenReturn(Optional.of(DELHI));
        when(geocodingProvider.resolve("Mumbai")).thenReturn(Optional.of(MUMBAI));
        when(geocodingProvider.resolve("Bengaluru")).thenReturn(Optional.of(BENGALURU));
        when(geocodingProvider.resolve("Kolkata")).thenReturn(Optional.of(KOLKATA));

        when(weatherProvider.getCurrentWeather(any(GeoLocation.class))).thenReturn(currentFixture());
        when(weatherProvider.getForecast(any(GeoLocation.class), anyInt())).thenReturn(forecastFixture());
    }

    @Test
    @DisplayName("Current weather query returns grounded current-weather answer")
    void currentWeatherQuery() throws Exception {
        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What's the weather in Delhi?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intent").value("CURRENT_WEATHER"))
                .andExpect(jsonPath("$.data.timeReference").value("NOW"))
                .andExpect(jsonPath("$.data.location.name").value("Delhi"))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("The current weather in Delhi")))
                .andExpect(jsonPath("$.data.currentWeather.temperature").value(28.0))
                .andExpect(jsonPath("$.data.forecast").doesNotExist());

        verify(weatherProvider).getCurrentWeather(DELHI);
    }

    @Test
    @DisplayName("Rain tomorrow query uses forecast data and generates rain answer with umbrella advisory")
    void rainTomorrowQuery() throws Exception {
        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Will it rain tomorrow in Mumbai?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("RAIN_QUERY"))
                .andExpect(jsonPath("$.data.timeReference").value("TOMORROW"))
                .andExpect(jsonPath("$.data.location.name").value("Mumbai"))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("Rain is likely tomorrow in Mumbai")))
                .andExpect(jsonPath("$.data.forecast.days.length()").value(2))
                .andExpect(jsonPath("$.data.currentWeather").doesNotExist())
                .andExpect(jsonPath("$.data.advisories[0]").value("Consider carrying an umbrella."));

        verify(weatherProvider).getForecast(MUMBAI, 2);
    }

    @Test
    @DisplayName("Temperature tomorrow query uses forecast data")
    void temperatureTomorrowQuery() throws Exception {
        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How hot will Bengaluru be tomorrow?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("TEMPERATURE_QUERY"))
                .andExpect(jsonPath("$.data.timeReference").value("TOMORROW"))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("will reach 30°C")))
                .andExpect(jsonPath("$.data.forecast").exists());

        verify(weatherProvider).getForecast(BENGALURU, 2);
    }

    @Test
    @DisplayName("Wind query uses current weather data")
    void windQuery() throws Exception {
        CurrentWeatherResponse windy = currentFixture();
        windy.setWindSpeed(45.0);
        when(weatherProvider.getCurrentWeather(any(GeoLocation.class))).thenReturn(windy);

        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Is it windy in Kolkata?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("WIND_QUERY"))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("The wind in Kolkata is currently 45 km/h")))
                .andExpect(jsonPath("$.data.currentWeather.windSpeed").value(45.0))
                .andExpect(jsonPath("$.data.advisories[0]").value(org.hamcrest.Matchers.containsString("Strong winds")));

        verify(weatherProvider).getCurrentWeather(KOLKATA);
    }

    @Test
    @DisplayName("Missing location returns controlled clarification and never calls the weather provider")
    void missingLocationClarification() throws Exception {
        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Will it rain tomorrow?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("Please specify the location")))
                .andExpect(jsonPath("$.data.intent").value("RAIN_QUERY"))
                .andExpect(jsonPath("$.data.location").doesNotExist());

        verify(weatherProvider, never()).getCurrentWeather(any(GeoLocation.class));
        verify(weatherProvider, never()).getForecast(any(GeoLocation.class), anyInt());
    }

    @Test
    @DisplayName("Invalid location returns controlled 404 via existing geocoding failure handling")
    void invalidLocationReturns404() throws Exception {
        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What's the weather in FakeCityXYZ?\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Location not found: FakeCityXYZ"));
    }

    @Test
    @DisplayName("Non-weather query returns controlled unsupported response without a fake answer")
    void unsupportedQuery() throws Exception {
        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Who won the cricket match?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.intent").value("UNSUPPORTED"))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("weather-related")));

        verify(weatherProvider, never()).getCurrentWeather(any(GeoLocation.class));
    }

    @Test
    @DisplayName("Historical query returns controlled unsupported-time response")
    void historicalTimeQuery() throws Exception {
        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What was the weather in Delhi 20 years ago?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timeReference").value("UNSUPPORTED"))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("Historical weather")));

        verify(weatherProvider, never()).getCurrentWeather(any(GeoLocation.class));
        verify(weatherProvider, never()).getForecast(any(GeoLocation.class), anyInt());
    }

    @Test
    @DisplayName("Blank message returns 400 validation error")
    void blankMessageReturns400() throws Exception {
        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Provider failure returns controlled 503 without stack traces")
    void providerFailureReturns503() throws Exception {
        when(weatherProvider.getCurrentWeather(any(GeoLocation.class)))
                .thenThrow(new WeatherProviderException("Weather service is temporarily unavailable. Please try again later."));

        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What's the weather in Delhi?\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Weather service is temporarily unavailable. Please try again later."));
    }

    @Test
    @DisplayName("Chat endpoint is publicly accessible without authentication")
    void chatEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/chat/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What's the weather in Delhi?\"}"))
                .andExpect(status().isOk());
    }
}