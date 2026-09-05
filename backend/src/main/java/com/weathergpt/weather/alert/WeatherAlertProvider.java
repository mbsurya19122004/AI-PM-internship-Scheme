package com.weathergpt.weather.alert;

import com.weathergpt.dto.alert.WeatherAlertDto;
import com.weathergpt.weather.model.GeoLocation;

import java.util.List;

/**
 * Abstraction over external extreme weather alert data sources.
 *
 * Implementations normalize provider-specific alert formats into the stable
 * WeatherGPT {@link WeatherAlertDto}. This interface supports multiple concurrent
 * providers — each implementation handles one data source.
 *
 * Future implementations may include:
 * - IMD (India Meteorological Department) official warning feeds
 * - CAP (Common Alerting Protocol) feeds
 * - NDMA alert feeds
 * - Open-Meteo UV/weather index advisories
 *
 * IMPORTANT: Implementations must correctly set {@link WeatherAlertDto#isOfficial()}
 * and {@link WeatherAlertDto#getInformationClass()} to accurately reflect whether
 * an alert is a verified official warning or an automated advisory.
 */
public interface WeatherAlertProvider {

    /**
     * Provider display name (for logging and status reporting).
     */
    String getProviderName();

    /**
     * Whether this provider supplies verified official government warnings.
     * Automated advisory providers must return false.
     */
    boolean isOfficialSource();

    /**
     * Retrieve active alerts/advisories for the given location.
     *
     * @param location resolved geographic coordinates
     * @return list of normalized alerts; empty list if none active (never null)
     * @throws com.weathergpt.exception.WeatherProviderException on provider failure
     */
    List<WeatherAlertDto> getAlerts(GeoLocation location);
}
