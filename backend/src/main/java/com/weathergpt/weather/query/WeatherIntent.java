package com.weathergpt.weather.query;

/**
 * Intent categories recognized for natural-language weather queries.
 */
public enum WeatherIntent {

    /** "What's the weather in Delhi?" */
    CURRENT_WEATHER,

    /** "Weather tomorrow in Chennai" */
    FORECAST,

    /** "Will it rain tomorrow in Mumbai?" / "Do I need an umbrella?" */
    RAIN_QUERY,

    /** "How hot will Bengaluru be tomorrow?" */
    TEMPERATURE_QUERY,

    /** "Is it windy in Kolkata?" */
    WIND_QUERY,

    /** "How humid is Kolkata?" */
    HUMIDITY_QUERY,

    /** Weather-related query that does not match a specialized intent. */
    GENERAL_WEATHER,

    /** Not a weather-related query at all. */
    UNSUPPORTED
}
