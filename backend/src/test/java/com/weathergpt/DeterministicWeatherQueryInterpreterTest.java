package com.weathergpt;

import com.weathergpt.weather.query.DeterministicWeatherQueryInterpreter;
import com.weathergpt.weather.query.ParsedWeatherQuery;
import com.weathergpt.weather.query.TimeReference;
import com.weathergpt.weather.query.WeatherIntent;
import com.weathergpt.weather.query.WeatherQueryInterpreter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicWeatherQueryInterpreterTest {

    private final WeatherQueryInterpreter interpreter = new DeterministicWeatherQueryInterpreter();

    private ParsedWeatherQuery parse(String message) {
        return interpreter.interpret(message);
    }

    @Test
    @DisplayName("Current weather query extracts intent, location, and NOW")
    void currentWeatherQuery() {
        ParsedWeatherQuery query = parse("What's the weather in Delhi?");
        assertEquals(WeatherIntent.CURRENT_WEATHER, query.getIntent());
        assertEquals("Delhi", query.getLocationQuery());
        assertEquals(TimeReference.NOW, query.getTimeReference());
    }

    @Test
    @DisplayName("Rain tomorrow query extracts RAIN_QUERY, Mumbai, TOMORROW")
    void rainTomorrowQuery() {
        ParsedWeatherQuery query = parse("Will it rain tomorrow in Mumbai?");
        assertEquals(WeatherIntent.RAIN_QUERY, query.getIntent());
        assertEquals("Mumbai", query.getLocationQuery());
        assertEquals(TimeReference.TOMORROW, query.getTimeReference());
    }

    @Test
    @DisplayName("Temperature tomorrow query extracts TEMPERATURE_QUERY, Bengaluru, TOMORROW")
    void temperatureTomorrowQuery() {
        ParsedWeatherQuery query = parse("How hot will Bengaluru be tomorrow?");
        assertEquals(WeatherIntent.TEMPERATURE_QUERY, query.getIntent());
        assertEquals("Bengaluru", query.getLocationQuery());
        assertEquals(TimeReference.TOMORROW, query.getTimeReference());
    }

    @Test
    @DisplayName("Wind query extracts WIND_QUERY, Kolkata, NOW")
    void windQuery() {
        ParsedWeatherQuery query = parse("Is it windy in Kolkata?");
        assertEquals(WeatherIntent.WIND_QUERY, query.getIntent());
        assertEquals("Kolkata", query.getLocationQuery());
        assertEquals(TimeReference.NOW, query.getTimeReference());
    }

    @Test
    @DisplayName("Humidity query extracts HUMIDITY_QUERY")
    void humidityQuery() {
        ParsedWeatherQuery query = parse("How humid is Kolkata?");
        assertEquals(WeatherIntent.HUMIDITY_QUERY, query.getIntent());
        assertEquals("Kolkata", query.getLocationQuery());
    }

    @Test
    @DisplayName("Forecast phrasing with tomorrow maps to FORECAST")
    void forecastPhrasing() {
        ParsedWeatherQuery query = parse("Weather tomorrow in Chennai");
        assertEquals(WeatherIntent.FORECAST, query.getIntent());
        assertEquals("Chennai", query.getLocationQuery());
        assertEquals(TimeReference.TOMORROW, query.getTimeReference());
    }

    @Test
    @DisplayName("Tomorrow's weather for Chennai extracts via preposition")
    void tomorrowWeatherFor() {
        ParsedWeatherQuery query = parse("Tell me tomorrow's weather for Chennai.");
        assertEquals(WeatherIntent.FORECAST, query.getIntent());
        assertEquals("Chennai", query.getLocationQuery());
        assertEquals(TimeReference.TOMORROW, query.getTimeReference());
    }

    @Test
    @DisplayName("Umbrella query maps to RAIN_QUERY")
    void umbrellaQuery() {
        ParsedWeatherQuery query = parse("Will I need an umbrella in Delhi tomorrow?");
        assertEquals(WeatherIntent.RAIN_QUERY, query.getIntent());
        assertEquals("Delhi", query.getLocationQuery());
        assertEquals(TimeReference.TOMORROW, query.getTimeReference());
    }

    @Test
    @DisplayName("Umbrella query without location leaves location null")
    void umbrellaQueryWithoutLocation() {
        ParsedWeatherQuery query = parse("Do I need an umbrella?");
        assertEquals(WeatherIntent.RAIN_QUERY, query.getIntent());
        assertNull(query.getLocationQuery());
    }

    @Test
    @DisplayName("Rain today maps to TODAY")
    void rainTodayQuery() {
        ParsedWeatherQuery query = parse("Will it rain today in Delhi?");
        assertEquals(WeatherIntent.RAIN_QUERY, query.getIntent());
        assertEquals(TimeReference.TODAY, query.getTimeReference());
    }

    @Test
    @DisplayName("This week maps to THIS_WEEK")
    void thisWeekQuery() {
        ParsedWeatherQuery query = parse("Will it rain this week in Delhi?");
        assertEquals(TimeReference.THIS_WEEK, query.getTimeReference());
        assertEquals(WeatherIntent.RAIN_QUERY, query.getIntent());
    }

    @Test
    @DisplayName("This weekend maps to THIS_WEEKEND")
    void thisWeekendQuery() {
        ParsedWeatherQuery query = parse("How hot will Bengaluru be this weekend?");
        assertEquals(TimeReference.THIS_WEEKEND, query.getTimeReference());
        assertEquals(WeatherIntent.TEMPERATURE_QUERY, query.getIntent());
        assertEquals("Bengaluru", query.getLocationQuery());
    }

    @Test
    @DisplayName("Right now maps to NOW")
    void rightNowQuery() {
        ParsedWeatherQuery query = parse("How hot is Mumbai right now?");
        assertEquals(TimeReference.NOW, query.getTimeReference());
        assertEquals("Mumbai", query.getLocationQuery());
    }

    @Test
    @DisplayName("Next day maps to NEXT_DAY")
    void nextDayQuery() {
        ParsedWeatherQuery query = parse("What will the weather be the next day in Delhi?");
        assertEquals(TimeReference.NEXT_DAY, query.getTimeReference());
        assertEquals("Delhi", query.getLocationQuery());
    }

    @Test
    @DisplayName("Multi-word dictionary location is extracted")
    void multiWordLocation() {
        ParsedWeatherQuery query = parse("Will it rain in New York tomorrow?");
        assertEquals("New York", query.getLocationQuery());
        assertEquals(TimeReference.TOMORROW, query.getTimeReference());
    }

    @Test
    @DisplayName("Non-weather query maps to UNSUPPORTED")
    void unsupportedQuery() {
        ParsedWeatherQuery query = parse("Who won the cricket match?");
        assertEquals(WeatherIntent.UNSUPPORTED, query.getIntent());
        assertNull(query.getLocationQuery());
    }

    @Test
    @DisplayName("Historical query maps to UNSUPPORTED time")
    void historicalQuery() {
        ParsedWeatherQuery query = parse("What was the weather in Delhi 20 years ago?");
        assertEquals(TimeReference.UNSUPPORTED, query.getTimeReference());
        assertEquals(WeatherIntent.CURRENT_WEATHER, query.getIntent());
    }

    @Test
    @DisplayName("HTML input is sanitized before parsing")
    void sanitizedInput() {
        ParsedWeatherQuery query = parse("<script>alert('x')</script>What's the weather in Delhi?");
        assertEquals(WeatherIntent.CURRENT_WEATHER, query.getIntent());
        assertEquals("Delhi", query.getLocationQuery());
    }
}