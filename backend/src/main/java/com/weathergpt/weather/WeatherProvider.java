package com.weathergpt.weather;

import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.weather.model.GeoLocation;

/**
 * Abstraction over external weather data sources.
 * Implementations normalize provider-specific payloads into the stable
 * WeatherGPT public DTOs, so swapping providers never changes the API contract.
 */
public interface WeatherProvider {

    CurrentWeatherResponse getCurrentWeather(GeoLocation location);

    ForecastResponse getForecast(GeoLocation location, int forecastDays);
}
