package com.weathergpt.service;

import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.exception.ResourceNotFoundException;
import com.weathergpt.weather.GeocodingProvider;
import com.weathergpt.weather.WeatherProvider;
import com.weathergpt.weather.model.GeoLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the weather vertical slice:
 * location query -> geocoding -> weather provider -> normalized response.
 *
 * GeoLocation-based overloads let callers (e.g. the natural-language layer)
 * resolve a location once and reuse it without a second geocoding call.
 */
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final GeocodingProvider geocodingProvider;
    private final WeatherProvider weatherProvider;

    public CurrentWeatherResponse getCurrentWeather(String location) {
        return getCurrentWeather(resolveLocation(location));
    }

    public CurrentWeatherResponse getCurrentWeather(GeoLocation location) {
        return weatherProvider.getCurrentWeather(location);
    }

    public ForecastResponse getForecast(String location, int forecastDays) {
        return getForecast(resolveLocation(location), forecastDays);
    }

    public ForecastResponse getForecast(GeoLocation location, int forecastDays) {
        return weatherProvider.getForecast(location, forecastDays);
    }

    public GeoLocation resolveLocation(String location) {
        String query = location.trim();
        return geocodingProvider.resolve(query)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + query));
    }
}
