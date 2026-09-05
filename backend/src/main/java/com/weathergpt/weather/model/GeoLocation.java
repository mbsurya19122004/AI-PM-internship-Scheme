package com.weathergpt.weather.model;

import lombok.*;

/**
 * Internal normalized location used across the weather pipeline.
 * Geocoding providers produce it; weather providers consume it.
 * Not exposed directly to API clients (see {@code dto.weather.LocationInfo}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoLocation {

    private String name;
    private Double latitude;
    private Double longitude;
    private String admin1;
    private String country;
    private String timezone;
}
