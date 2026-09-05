package com.weathergpt.dto.weather;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Normalized multi-day weather forecast for a location.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForecastResponse {

    private LocationInfo location;
    private String timezone;
    private List<ForecastDay> days;
    private String provider;
}
