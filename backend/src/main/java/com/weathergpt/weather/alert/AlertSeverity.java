package com.weathergpt.weather.alert;

/**
 * Normalized severity classification for weather alerts.
 *
 * Provider-specific severity levels (e.g., "Red", "Orange", "Yellow", CAP severity codes)
 * must be normalized to these values before being surfaced in API responses.
 * This ensures consistent consumer behaviour regardless of the data source.
 */
public enum AlertSeverity {

    /** Informational — general awareness. No immediate threat. */
    LOW,

    /** Moderate conditions expected. Precaution recommended. */
    MODERATE,

    /** Severe conditions. Action strongly recommended. */
    SEVERE,

    /** Extreme conditions. Immediate action may be required. */
    EXTREME,

    /** Severity could not be determined from the source data. */
    UNKNOWN
}
