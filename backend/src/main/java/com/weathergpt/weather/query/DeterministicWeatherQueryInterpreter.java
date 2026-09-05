package com.weathergpt.weather.query;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic natural-language weather query interpreter.
 *
 * Uses keyword matching, regular expressions, and a small city dictionary —
 * no external LLM required. This keeps Phase 2 fast, offline-testable, and
 * free of any API-key dependency.
 */
@Service
public class DeterministicWeatherQueryInterpreter implements WeatherQueryInterpreter {

    private static final Set<String> WEATHER_KEYWORDS = Set.of(
            "weather", "rain", "raining", "rainy", "umbrella", "precipitation", "shower", "drizzle",
            "hot", "cold", "warm", "cool", "temperature", "degrees", "wind", "windy", "breeze", "gust",
            "blowing", "humid", "humidity", "muggy", "sticky", "moist", "sunny", "cloudy", "overcast",
            "fog", "foggy", "forecast", "storm", "thunder", "snow", "clear", "chilly", "freezing"
    );

    private static final Set<String> RAIN_KEYWORDS = Set.of(
            "rain", "raining", "rainy", "umbrella", "precipitation", "shower", "drizzle", "downpour"
    );

    private static final Set<String> TEMPERATURE_KEYWORDS = Set.of(
            "hot", "cold", "warm", "cool", "temperature", "degrees", "heat", "chilly", "freezing"
    );

    private static final Set<String> WIND_KEYWORDS = Set.of(
            "wind", "windy", "breeze", "gust", "blowing"
    );

    private static final Set<String> HUMIDITY_KEYWORDS = Set.of(
            "humid", "humidity", "muggy", "sticky", "moist"
    );

    private static final Pattern HISTORICAL_TIME = Pattern.compile(
            "\\b(ago|yesterday|last\\s+(week|month|year|night|day)|past|historical)\\b");
    private static final Pattern WEEKEND_TIME = Pattern.compile("\\b(this\\s+)?weekend\\b");
    private static final Pattern WEEK_TIME = Pattern.compile("\\b(this\\s+)?week\\b");
    private static final Pattern TOMORROW_TIME = Pattern.compile("\\btomorrow\\b|\\btmrw\\b");
    private static final Pattern NEXT_DAY_TIME = Pattern.compile("\\bnext\\s+day\\b");
    private static final Pattern TONIGHT_TIME = Pattern.compile("\\btonight\\b");
    private static final Pattern TODAY_TIME = Pattern.compile("\\btoday\\b");
    private static final Pattern NOW_TIME = Pattern.compile("right\\s+now|\\bnow\\b|\\bcurrently\\b|at the moment");

    /** Extracts the phrase after a location preposition, stopping at time words or punctuation. */
    private static final Pattern PREPOSITION_LOCATION = Pattern.compile(
            "\\b(?:in|at|for|of|near)\\s+([a-z][a-z .'\\-]{1,60}?)"
            + "(?=\\s*(?:right\\s+now|\\bnow\\b|\\btoday\\b|\\btomorrow\\b|\\btonight\\b|\\bthis\\s+week(?:end)?\\b|\\bnext\\b|[?.,;!]|$))",
            Pattern.CASE_INSENSITIVE);

    /** Common place names (lowercase -> geocoding query) so cities without a preposition are still found. */
    private static final Map<String, String> CITY_DICTIONARY = buildCityDictionary();

    @Override
    public ParsedWeatherQuery interpret(String message) {
        String original = message == null ? "" : message;
        String normalized = normalize(message);
        TimeReference timeReference = detectTimeReference(normalized);
        WeatherIntent intent = detectIntent(normalized, timeReference);

        return ParsedWeatherQuery.builder()
                .intent(intent)
                .timeReference(timeReference)
                .aspect(detectAspect(normalized))
                .locationQuery(intent == WeatherIntent.UNSUPPORTED ? null : extractLocation(normalized, original))
                .build();
    }

    private WeatherIntent detectIntent(String text, TimeReference time) {
        if (containsAny(text, WEATHER_KEYWORDS) == false) {
            return WeatherIntent.UNSUPPORTED;
        }
        if (containsAny(text, RAIN_KEYWORDS)) {
            return WeatherIntent.RAIN_QUERY;
        }
        if (containsAny(text, TEMPERATURE_KEYWORDS)) {
            return WeatherIntent.TEMPERATURE_QUERY;
        }
        if (containsAny(text, WIND_KEYWORDS)) {
            return WeatherIntent.WIND_QUERY;
        }
        if (containsAny(text, HUMIDITY_KEYWORDS)) {
            return WeatherIntent.HUMIDITY_QUERY;
        }
        if (text.contains("forecast")
                || time == TimeReference.TOMORROW
                || time == TimeReference.THIS_WEEK
                || time == TimeReference.THIS_WEEKEND) {
            return WeatherIntent.FORECAST;
        }
        if (text.contains("weather")) {
            return WeatherIntent.CURRENT_WEATHER;
        }
        return WeatherIntent.GENERAL_WEATHER;
    }

    private TimeReference detectTimeReference(String text) {
        if (HISTORICAL_TIME.matcher(text).find()) {
            return TimeReference.UNSUPPORTED;
        }
        if (WEEKEND_TIME.matcher(text).find()) {
            return TimeReference.THIS_WEEKEND;
        }
        if (WEEK_TIME.matcher(text).find()) {
            return TimeReference.THIS_WEEK;
        }
        if (NEXT_DAY_TIME.matcher(text).find()) {
            return TimeReference.NEXT_DAY;
        }
        if (TOMORROW_TIME.matcher(text).find()) {
            return TimeReference.TOMORROW;
        }
        if (TONIGHT_TIME.matcher(text).find()) {
            return TimeReference.TODAY;
        }
        if (TODAY_TIME.matcher(text).find()) {
            return TimeReference.TODAY;
        }
        if (NOW_TIME.matcher(text).find()) {
            return TimeReference.NOW;
        }
        return TimeReference.NOW;
    }

    private WeatherAspect detectAspect(String text) {
        if (containsAny(text, RAIN_KEYWORDS)) {
            return WeatherAspect.PRECIPITATION;
        }
        if (containsAny(text, TEMPERATURE_KEYWORDS)) {
            return WeatherAspect.TEMPERATURE;
        }
        if (containsAny(text, WIND_KEYWORDS)) {
            return WeatherAspect.WIND;
        }
        if (containsAny(text, HUMIDITY_KEYWORDS)) {
            return WeatherAspect.HUMIDITY;
        }
        return WeatherAspect.GENERAL;
    }

    private String extractLocation(String text, String original) {
        for (Map.Entry<String, String> entry : CITY_DICTIONARY.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        // Match against the original message so extracted names keep their casing.
        Matcher matcher = PREPOSITION_LOCATION.matcher(original);
        if (matcher.find()) {
            String location = matcher.group(1).trim();
            if (!location.isEmpty() && !containsAny(location.toLowerCase(), WEATHER_KEYWORDS)) {
                return location;
            }
        }
        return null;
    }

    private static String normalize(String message) {
        if (message == null) {
            return "";
        }
        // Strip HTML tags before parsing (input sanitization convention)
        String stripped = message.replaceAll("<[^>]*>", " ");
        return stripped.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private static boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> buildCityDictionary() {
        Map<String, String> cities = new LinkedHashMap<>();
        // India
        cities.put("new delhi", "Delhi");
        cities.put("delhi", "Delhi");
        cities.put("mumbai", "Mumbai");
        cities.put("bombay", "Mumbai");
        cities.put("bengaluru", "Bengaluru");
        cities.put("bangalore", "Bengaluru");
        cities.put("chennai", "Chennai");
        cities.put("madras", "Chennai");
        cities.put("kolkata", "Kolkata");
        cities.put("calcutta", "Kolkata");
        cities.put("hyderabad", "Hyderabad");
        cities.put("pune", "Pune");
        cities.put("ahmedabad", "Ahmedabad");
        cities.put("jaipur", "Jaipur");
        cities.put("lucknow", "Lucknow");
        cities.put("surat", "Surat");
        cities.put("kanpur", "Kanpur");
        cities.put("nagpur", "Nagpur");
        cities.put("indore", "Indore");
        cities.put("bhopal", "Bhopal");
        cities.put("patna", "Patna");
        cities.put("chandigarh", "Chandigarh");
        cities.put("kochi", "Kochi");
        cities.put("cochin", "Kochi");
        cities.put("goa", "Goa");
        cities.put("shimla", "Shimla");
        // World
        cities.put("new york", "New York");
        cities.put("nyc", "New York");
        cities.put("los angeles", "Los Angeles");
        cities.put("san francisco", "San Francisco");
        cities.put("chicago", "Chicago");
        cities.put("london", "London");
        cities.put("paris", "Paris");
        cities.put("tokyo", "Tokyo");
        cities.put("singapore", "Singapore");
        cities.put("dubai", "Dubai");
        cities.put("sydney", "Sydney");
        cities.put("melbourne", "Melbourne");
        cities.put("berlin", "Berlin");
        cities.put("rome", "Rome");
        cities.put("madrid", "Madrid");
        cities.put("bangkok", "Bangkok");
        cities.put("seoul", "Seoul");
        cities.put("toronto", "Toronto");
        cities.put("moscow", "Moscow");
        cities.put("beijing", "Beijing");
        cities.put("shanghai", "Shanghai");
        cities.put("istanbul", "Istanbul");
        cities.put("karachi", "Karachi");
        cities.put("dhaka", "Dhaka");
        cities.put("kathmandu", "Kathmandu");
        cities.put("colombo", "Colombo");
        cities.put("cairo", "Cairo");
        cities.put("cape town", "Cape Town");
        cities.put("mexico city", "Mexico City");
        cities.put("rio de janeiro", "Rio de Janeiro");
        return cities;
    }
}
