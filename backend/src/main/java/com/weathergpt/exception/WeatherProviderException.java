package com.weathergpt.exception;

/**
 * Thrown when an external weather/geocoding provider fails or is unavailable.
 * Mapped to HTTP 503 by {@link GlobalExceptionHandler} so callers receive a
 * controlled "temporarily unavailable" response instead of raw provider errors.
 */
public class WeatherProviderException extends RuntimeException {

    public WeatherProviderException(String message) {
        super(message);
    }

    public WeatherProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
