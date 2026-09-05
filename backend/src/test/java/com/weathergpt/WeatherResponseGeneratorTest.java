package com.weathergpt;

import com.weathergpt.dto.chat.ChatResponse;
import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastDay;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.service.WeatherResponseGenerator;
import com.weathergpt.weather.model.GeoLocation;
import com.weathergpt.weather.query.ParsedWeatherQuery;
import com.weathergpt.weather.query.TimeReference;
import com.weathergpt.weather.query.WeatherAspect;
import com.weathergpt.weather.query.WeatherIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeatherResponseGeneratorTest {

    private final WeatherResponseGenerator generator = new WeatherResponseGenerator();

    private static final GeoLocation DELHI = GeoLocation.builder()
            .name("Delhi").latitude(28.65).longitude(77.23).build();

    private ParsedWeatherQuery query(WeatherIntent intent, TimeReference time, WeatherAspect aspect) {
        return ParsedWeatherQuery.builder()
                .intent(intent)
                .timeReference(time)
                .aspect(aspect)
                .build();
    }

    @Test
    @DisplayName("High precipitation produces rain answer and umbrella advisory")
    void highPrecipitationGeneratesUmbrellaAdvisory() {
        ForecastDay day = ForecastDay.builder()
                .date("2026-09-05")
                .precipitationProbabilityMax(75)
                .weatherDescription("Slight rain")
                .tempMax(30.0)
                .tempMin(24.0)
                .build();
        ForecastResponse forecast = ForecastResponse.builder().days(List.of(day)).build();

        ChatResponse response = generator.forecastDay(
                query(WeatherIntent.RAIN_QUERY, TimeReference.TOMORROW, WeatherAspect.PRECIPITATION),
                DELHI, forecast, day, TimeReference.TOMORROW);

        assertTrue(response.getAnswer().contains("Rain is likely tomorrow in Delhi with a 75% chance"));
        assertTrue(response.getAdvisories().contains("Consider carrying an umbrella."));
        assertEquals(WeatherIntent.RAIN_QUERY, response.getIntent());
        assertEquals("Delhi", response.getLocation().getName());
    }

    @Test
    @DisplayName("Low precipitation does not generate an umbrella advisory")
    void lowPrecipitationNoUmbrellaAdvisory() {
        ForecastDay day = ForecastDay.builder()
                .date("2026-09-05")
                .precipitationProbabilityMax(10)
                .weatherDescription("Partly cloudy")
                .tempMax(30.0)
                .tempMin(24.0)
                .build();
        ForecastResponse forecast = ForecastResponse.builder().days(List.of(day)).build();

        ChatResponse response = generator.forecastDay(
                query(WeatherIntent.RAIN_QUERY, TimeReference.TOMORROW, WeatherAspect.PRECIPITATION),
                DELHI, forecast, day, TimeReference.TOMORROW);

        assertTrue(response.getAnswer().contains("Rain is unlikely tomorrow in Delhi"));
        assertFalse(response.getAdvisories().contains("Consider carrying an umbrella."));
    }

    @Test
    @DisplayName("Very high temperature produces heat advisory")
    void highTemperatureGeneratesHeatAdvisory() {
        ForecastDay day = ForecastDay.builder()
                .date("2026-09-05")
                .tempMax(42.0)
                .tempMin(28.0)
                .weatherDescription("Clear sky")
                .precipitationProbabilityMax(5)
                .build();
        ForecastResponse forecast = ForecastResponse.builder().days(List.of(day)).build();

        ChatResponse response = generator.forecastDay(
                query(WeatherIntent.TEMPERATURE_QUERY, TimeReference.TOMORROW, WeatherAspect.TEMPERATURE),
                DELHI, forecast, day, TimeReference.TOMORROW);

        assertTrue(response.getAnswer().contains("will reach 42°C with a low of 28°C"));
        assertTrue(response.getAdvisories().stream()
                .anyMatch(a -> a.contains("Stay hydrated")));
    }

    @Test
    @DisplayName("Strong current wind produces wind caution advisory")
    void strongWindGeneratesWindAdvisory() {
        CurrentWeatherResponse current = CurrentWeatherResponse.builder()
                .temperature(26.0)
                .windSpeed(48.0)
                .windDirection(250)
                .weatherDescription("Partly cloudy")
                .build();

        ChatResponse response = generator.currentWeather(
                query(WeatherIntent.WIND_QUERY, TimeReference.NOW, WeatherAspect.WIND),
                DELHI, current);

        assertTrue(response.getAnswer().contains("The wind in Delhi is currently 48 km/h"));
        assertTrue(response.getAdvisories().stream()
                .anyMatch(a -> a.contains("Strong winds")));
    }

    @Test
    @DisplayName("Moderate conditions produce no advisories")
    void moderateConditionsNoAdvisories() {
        CurrentWeatherResponse current = CurrentWeatherResponse.builder()
                .temperature(26.0)
                .windSpeed(12.0)
                .humidity(55)
                .weatherDescription("Partly cloudy")
                .build();

        ChatResponse response = generator.currentWeather(
                query(WeatherIntent.CURRENT_WEATHER, TimeReference.NOW, WeatherAspect.GENERAL),
                DELHI, current);

        assertTrue(response.getAdvisories().isEmpty());
        assertTrue(response.getAnswer().contains("The current weather in Delhi is partly cloudy"));
    }

    @Test
    @DisplayName("Weekend answer lists Saturday and Sunday days")
    void weekendAnswerListsDays() {
        ForecastDay saturday = ForecastDay.builder().date("2026-09-05")
                .weatherDescription("Partly cloudy").tempMax(31.0).tempMin(23.0).build();
        ForecastDay sunday = ForecastDay.builder().date("2026-09-06")
                .weatherDescription("Moderate rain").tempMax(29.0).tempMin(23.0)
                .precipitationProbabilityMax(80).build();

        ChatResponse response = generator.forecastWeekend(
                query(WeatherIntent.FORECAST, TimeReference.THIS_WEEKEND, WeatherAspect.GENERAL),
                DELHI, List.of(saturday, sunday));

        assertTrue(response.getAnswer().contains("This weekend in Delhi"));
        assertTrue(response.getAnswer().contains("Saturday"));
        assertTrue(response.getAnswer().contains("Sunday"));
        assertTrue(response.getAdvisories().contains("Consider carrying an umbrella."));
    }
}