package com.weathergpt.dto.weather;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Normalized real-time weather conditions at a location.
 * Units: temperature °C, wind speed km/h, wind direction degrees,
 * humidity %, pressure hPa, visibility meters.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentWeatherResponse {

    private LocationInfo location;
    private Double temperature;
    private Double apparentTemperature;
    private Integer humidity;
    private Double windSpeed;
    private Integer windDirection;
    private Integer weatherCode;
    private String weatherDescription;
    private Double pressure;
    private Double visibility;
    private Boolean isDay;
    private String observedAt;
    private String timezone;
    private String provider;
}
