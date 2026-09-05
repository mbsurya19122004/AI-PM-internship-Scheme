package com.weathergpt.dto.weather;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Normalized weather forecast for one day.
 * Temperatures in °C, precipitation probability in %, wind speed km/h.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForecastDay {

    private String date;
    private Integer weatherCode;
    private String weatherDescription;
    private Double tempMax;
    private Double tempMin;
    private Integer precipitationProbabilityMax;
    private Double precipitationSum;
    private Double windSpeedMax;
    private Integer humidityMax;
}
