package com.weathergpt.dto.alert;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Top-level response for the /api/alerts endpoint.
 * Includes the list of alerts plus metadata about the query and provider status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertResponse {

    /** The location string used to query alerts. */
    private String location;

    /** Resolved latitude of the queried location. */
    private Double latitude;

    /** Resolved longitude of the queried location. */
    private Double longitude;

    /** The list of active alerts and advisories for the location. */
    private List<WeatherAlertDto> alerts;

    /** Total count of alerts returned. */
    private int totalAlerts;

    /**
     * Provider status message.
     * If no official alert provider is configured, this will indicate that
     * official alert integration is pending and describe the current provider status.
     */
    private String providerStatus;

    /**
     * True if at least one configured official alert provider is active.
     * False when running in advisory-only mode (no official provider integrated).
     */
    private boolean officialProviderActive;
}
