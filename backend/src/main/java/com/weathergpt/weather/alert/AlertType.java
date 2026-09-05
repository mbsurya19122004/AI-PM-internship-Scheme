package com.weathergpt.weather.alert;

/**
 * Normalized alert type classifications for extreme weather events.
 * Used to categorize alerts regardless of the source provider's naming convention.
 */
public enum AlertType {

    RAIN,
    HEAVY_RAIN,
    THUNDERSTORM,
    LIGHTNING,
    CYCLONE,
    STRONG_WIND,
    HEATWAVE,
    COLD_WAVE,
    FOG,
    FLOOD,
    LANDSLIDE,
    OTHER
}
