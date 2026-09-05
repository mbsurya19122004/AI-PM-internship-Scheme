package com.weathergpt.weather.query;

import lombok.*;

/**
 * Normalized interpretation of a natural-language weather query.
 * Internal model produced by {@link WeatherQueryInterpreter} and consumed by
 * the weather query orchestration layer. Not exposed directly in API responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedWeatherQuery {

    private WeatherIntent intent;
    private TimeReference timeReference;
    private WeatherAspect aspect;

    /** Extracted location text (e.g. "Delhi"); null when no location was detected. */
    private String locationQuery;
}
