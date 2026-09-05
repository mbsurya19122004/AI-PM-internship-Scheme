package com.weathergpt.dto.weather;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Normalized location information included in weather API responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationInfo {

    private String name;
    private Double latitude;
    private Double longitude;
    private String admin1;
    private String country;
    private String timezone;
}
