package com.weathergpt.weather.alert;

/**
 * Mandatory classification of every alert or advisory surfaced by WeatherGPT.
 *
 * This distinction is architecturally enforced to comply with the requirement
 * that automated advisories must NEVER be presented as official government warnings.
 *
 * <pre>
 * OFFICIAL_WARNING  — Originates from a verified official government source
 *                     (e.g. IMD, NDMA). Must include source, severity, issued time,
 *                     validity period, and affected location.
 *
 * AUTOMATED_ADVISORY — Generated automatically from weather data thresholds by
 *                      WeatherGPT. Must be clearly labelled as a system-generated
 *                      advisory and not as an official government warning.
 *
 * OBSERVATION       — A factual statement derived from observed weather conditions.
 *                     Not a warning or advisory.
 * </pre>
 */
public enum AlertInformationClass {

    /**
     * Originates from a verified official source (government meteorological agency).
     * Must include: source, severity, issued time, validity, affected location.
     */
    OFFICIAL_WARNING,

    /**
     * System-generated advisory based on weather data thresholds.
     * Must be clearly labelled as automated. Never present as an official warning.
     */
    AUTOMATED_ADVISORY,

    /**
     * A factual description of observed weather conditions.
     * Not a warning or advisory.
     */
    OBSERVATION
}
