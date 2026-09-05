package com.weathergpt.controller;

import com.weathergpt.dto.ApiResponse;
import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only weather endpoints.
 * Weather data is public information, so no authentication is required
 * (see SecurityConfig). Authentication remains required for account features.
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    public static final int DEFAULT_FORECAST_DAYS = 7;
    public static final int MAX_FORECAST_DAYS = 16;

    private final WeatherService weatherService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<CurrentWeatherResponse>> getCurrentWeather(
            @RequestParam(required = false) String location) {
        validateLocation(location);
        return ResponseEntity.ok(ApiResponse.success("Current weather retrieved",
                weatherService.getCurrentWeather(location)));
    }

    @GetMapping("/forecast")
    public ResponseEntity<ApiResponse<ForecastResponse>> getForecast(
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "7") int days) {
        validateLocation(location);
        int forecastDays = Math.max(1, Math.min(days, MAX_FORECAST_DAYS));
        return ResponseEntity.ok(ApiResponse.success("Weather forecast retrieved",
                weatherService.getForecast(location, forecastDays)));
    }

    private void validateLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Location is required");
        }
    }
}
