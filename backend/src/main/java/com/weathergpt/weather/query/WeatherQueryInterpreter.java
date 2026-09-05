package com.weathergpt.weather.query;

/**
 * Turns a natural-language message into a normalized {@link ParsedWeatherQuery}.
 *
 * The current implementation is deterministic. A future LLM-based interpreter
 * (OpenAI/Gemini/Llama) can implement this same interface without changing the
 * rest of the pipeline.
 */
public interface WeatherQueryInterpreter {

    ParsedWeatherQuery interpret(String message);
}
