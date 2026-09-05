package com.weathergpt.dto.alert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weathergpt.weather.alert.AlertInformationClass;
import com.weathergpt.weather.alert.AlertSeverity;
import com.weathergpt.weather.alert.AlertType;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Normalized extreme weather alert / advisory DTO.
 *
 * Every alert exposed by the API must populate:
 * - informationClass (OFFICIAL_WARNING, AUTOMATED_ADVISORY, or OBSERVATION)
 * - severity
 * - title and description
 *
 * Additional fields are optional depending on the source provider.
 *
 * IMPORTANT: The informationClass field must be presented to the user.
 * Automated advisories must never be displayed as official government warnings.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeatherAlertDto {

    /** Unique alert identifier (provider-assigned or system-generated). */
    private String id;

    /** Short descriptive title of the alert or advisory. */
    private String title;

    /** Full description of the event, conditions, and recommended actions. */
    private String description;

    /**
     * MANDATORY classification — must be shown to users.
     * Distinguishes official government warnings from automated advisories.
     */
    private AlertInformationClass informationClass;

    /** Normalized alert type category. */
    private AlertType alertType;

    /** Normalized severity level. */
    private AlertSeverity severity;

    /**
     * Source attribution.
     * For official warnings: the issuing government agency (e.g., "India Meteorological Department").
     * For automated advisories: "WeatherGPT Automated Advisory System".
     * For observations: "WeatherGPT Weather Data".
     */
    private String source;

    /** True if this alert originates from a verified official government source. */
    private boolean official;

    /** UTC instant when the alert was issued by the source. */
    private Instant issuedAt;

    /** UTC instant when the alert conditions are expected to begin. */
    private Instant effectiveFrom;

    /** UTC instant when the alert expires. Null means no defined expiry. */
    private Instant effectiveUntil;

    /**
     * Geographic areas affected by this alert.
     * May contain district names, city names, or region descriptions.
     */
    private List<String> affectedLocations;

    /** Central latitude of the affected area (may be null for broad regional warnings). */
    private Double latitude;

    /** Central longitude of the affected area (may be null for broad regional warnings). */
    private Double longitude;

    /**
     * Any additional recommended actions or instructions derived from the alert.
     * Must not be presented as official evacuation orders unless source is official.
     */
    private List<String> additionalInstructions;
}
