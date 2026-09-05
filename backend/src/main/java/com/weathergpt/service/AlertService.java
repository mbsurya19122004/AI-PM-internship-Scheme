package com.weathergpt.service;

import com.weathergpt.dto.alert.AlertResponse;
import com.weathergpt.dto.alert.WeatherAlertDto;
import com.weathergpt.exception.WeatherProviderException;
import com.weathergpt.weather.GeocodingProvider;
import com.weathergpt.weather.alert.AlertInformationClass;
import com.weathergpt.weather.alert.WeatherAlertProvider;
import com.weathergpt.weather.model.GeoLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the alert vertical slice:
 * location resolution → alert provider → normalized alert response.
 *
 * Architectural rules enforced here:
 * 1. Every alert must have informationClass set by the provider.
 * 2. Official warnings (informationClass = OFFICIAL_WARNING) must originate
 *    from providers where isOfficialSource() == true.
 * 3. Automated advisories must be clearly classified as AUTOMATED_ADVISORY.
 * 4. No alerts are fabricated by this service.
 * 5. Provider failures are logged and handled gracefully — the service returns
 *    a meaningful response rather than propagating a raw provider exception.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private static final String NO_PROVIDER_STATUS =
            "Official extreme weather alert integration is pending. "
            + "No verified government alert provider is currently configured. "
            + "Check official sources such as mausam.imd.gov.in for current warnings.";

    private static final String PROVIDER_ACTIVE_STATUS =
            "Official alert provider active.";

    private final GeocodingProvider geocodingProvider;
    private final WeatherAlertProvider alertProvider;

    /**
     * Retrieve active alerts for the given location query string.
     *
     * @param locationQuery human-readable location (e.g. "Delhi", "Mumbai")
     * @return normalized alert response including provider status
     */
    public AlertResponse getAlerts(String locationQuery) {
        GeoLocation location = resolveLocation(locationQuery);
        return getAlertsForLocation(location);
    }

    /**
     * Retrieve active alerts for a resolved GeoLocation.
     * Allows the caller to reuse a geocoding result without a second API call.
     */
    public AlertResponse getAlertsForLocation(GeoLocation location) {
        boolean officialProviderActive = alertProvider.isOfficialSource();
        String providerStatus = officialProviderActive ? PROVIDER_ACTIVE_STATUS : NO_PROVIDER_STATUS;

        List<WeatherAlertDto> alerts = new ArrayList<>();

        try {
            List<WeatherAlertDto> providerAlerts = alertProvider.getAlerts(location);

            // Validate that non-official providers do not produce official warnings.
            // This is a safety check — providers should set informationClass correctly,
            // but we enforce the constraint at the service boundary.
            for (WeatherAlertDto alert : providerAlerts) {
                if (!alertProvider.isOfficialSource()
                        && AlertInformationClass.OFFICIAL_WARNING.equals(alert.getInformationClass())) {
                    log.warn("AlertService: Provider '{}' is not an official source but produced "
                            + "an OFFICIAL_WARNING alert '{}'. Reclassifying as AUTOMATED_ADVISORY.",
                            alertProvider.getProviderName(), alert.getId());
                    // Reclassify to prevent misleading the user
                    alert.setInformationClass(AlertInformationClass.AUTOMATED_ADVISORY);
                    alert.setOfficial(false);
                }
                alerts.add(alert);
            }

        } catch (WeatherProviderException e) {
            log.error("AlertService: Provider '{}' failed for location '{}': {}",
                    alertProvider.getProviderName(), location.getName(), e.getMessage());
            // Return response with provider error status rather than propagating exception
            providerStatus = "Alert provider temporarily unavailable. "
                    + "Please check official sources such as mausam.imd.gov.in.";
        }

        return AlertResponse.builder()
                .location(location.getName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .alerts(alerts)
                .totalAlerts(alerts.size())
                .providerStatus(providerStatus)
                .officialProviderActive(officialProviderActive)
                .build();
    }

    private GeoLocation resolveLocation(String locationQuery) {
        String query = locationQuery.trim();
        return geocodingProvider.resolve(query)
                .orElseThrow(() -> new com.weathergpt.exception.ResourceNotFoundException(
                        "Location not found: " + query));
    }
}
