package com.weathergpt.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.dto.weather.LocationInfo;
import com.weathergpt.weather.query.TimeReference;
import com.weathergpt.weather.query.WeatherIntent;
import lombok.*;

import java.util.List;

/**
 * Structured response for a natural-language weather query.
 * Includes the conversational answer plus structured data so mobile clients
 * can render weather cards. Only the fields relevant to the answer are set.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    /** Conversational, data-grounded answer. */
    private String answer;

    /** Recognized intent, or UNSUPPORTED for non-weather queries. */
    private WeatherIntent intent;

    /** Interpreted time reference. */
    private TimeReference timeReference;

    /** Resolved location (null when the query needs clarification). */
    private LocationInfo location;

    /** Present when the answer is based on real-time conditions. */
    private CurrentWeatherResponse currentWeather;

    /** Present when the answer is based on forecast data. */
    private ForecastResponse forecast;

    /** Simple data-driven weather advisories (not official warnings). */
    private List<String> advisories;
}
