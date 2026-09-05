package com.weathergpt.weather;

import com.weathergpt.weather.model.GeoLocation;

import java.util.Optional;

/**
 * Resolves human-readable location names to normalized coordinates.
 * Implementations wrap an external geocoding service.
 */
public interface GeocodingProvider {

    /**
     * Resolve a location query (e.g. "Delhi") to a normalized location.
     *
     * @return the resolved location, or {@link Optional#empty()} when the query matches nothing
     * @throws com.weathergpt.exception.WeatherProviderException when the geocoding service fails
     */
    Optional<GeoLocation> resolve(String query);
}
