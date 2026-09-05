package com.weathergpt.service;

import com.weathergpt.dto.chat.ChatResponse;
import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastDay;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.weather.model.GeoLocation;
import com.weathergpt.weather.query.ParsedWeatherQuery;
import com.weathergpt.weather.query.TimeReference;
import com.weathergpt.weather.query.WeatherIntent;
import com.weathergpt.weather.query.WeatherQueryInterpreter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Orchestrates the natural-language weather flow:
 *
 * user message → query interpretation → location resolution (existing
 * geocoding) → real weather data (existing WeatherService) → grounded response.
 *
 * Weather facts always come from the providers via {@link WeatherService} —
 * never from the query understanding layer.
 */
@Service
@RequiredArgsConstructor
public class WeatherQueryService {

    private static final int FORECAST_TOMORROW_DAYS = 2;
    private static final int FORECAST_WEEK_DAYS = 7;
    private static final int FORECAST_TODAY_DAYS = 1;

    private final WeatherQueryInterpreter interpreter;
    private final WeatherService weatherService;
    private final WeatherResponseGenerator responseGenerator;

    public ChatResponse processQuery(String message) {
        ParsedWeatherQuery parsed = interpreter.interpret(message);

        if (parsed.getIntent() == WeatherIntent.UNSUPPORTED) {
            return ChatResponse.builder()
                    .answer("I can only answer weather-related questions right now. Try asking something like "
                            + "\"What's the weather in Delhi?\" or \"Will it rain tomorrow in Mumbai?\".")
                    .intent(WeatherIntent.UNSUPPORTED)
                    .timeReference(parsed.getTimeReference())
                    .build();
        }

        if (parsed.getTimeReference() == TimeReference.UNSUPPORTED) {
            return ChatResponse.builder()
                    .answer("I can't provide weather for that time range yet. Historical weather is planned for a "
                            + "future phase. I currently support now, today, tomorrow, this week, and this weekend.")
                    .intent(parsed.getIntent())
                    .timeReference(TimeReference.UNSUPPORTED)
                    .build();
        }

        if (parsed.getLocationQuery() == null || parsed.getLocationQuery().isBlank()) {
            return ChatResponse.builder()
                    .answer("Please specify the location for which you want weather information. For example: "
                            + "\"Will it rain tomorrow in Delhi?\"")
                    .intent(parsed.getIntent())
                    .timeReference(parsed.getTimeReference())
                    .build();
        }

        GeoLocation location = weatherService.resolveLocation(parsed.getLocationQuery());
        return buildDataResponse(parsed, location);
    }

    private ChatResponse buildDataResponse(ParsedWeatherQuery parsed, GeoLocation location) {
        TimeReference time = parsed.getTimeReference();
        WeatherIntent intent = parsed.getIntent();

        switch (time) {
            case TOMORROW, NEXT_DAY -> {
                // NEXT_DAY is defined to behave exactly like TOMORROW.
                ForecastResponse forecast = weatherService.getForecast(location, FORECAST_TOMORROW_DAYS);
                return responseGenerator.forecastDay(parsed, location, forecast, dayAt(forecast, 1), time);
            }
            case THIS_WEEK -> {
                ForecastResponse forecast = weatherService.getForecast(location, FORECAST_WEEK_DAYS);
                return responseGenerator.forecastWeek(parsed, location, forecast);
            }
            case THIS_WEEKEND -> {
                ForecastResponse forecast = weatherService.getForecast(location, FORECAST_WEEK_DAYS);
                return responseGenerator.forecastWeekend(parsed, location, weekendDays(forecast));
            }
            default -> {
                if (intent == WeatherIntent.RAIN_QUERY) {
                    // Umbrella-style queries need today's precipitation probability,
                    // which comes from forecast data, not current conditions.
                    ForecastResponse forecast = weatherService.getForecast(location, FORECAST_TODAY_DAYS);
                    return responseGenerator.forecastDay(parsed, location, forecast, dayAt(forecast, 0), time);
                }
                CurrentWeatherResponse current = weatherService.getCurrentWeather(location);
                return responseGenerator.currentWeather(parsed, location, current);
            }
        }
    }

    private static ForecastDay dayAt(ForecastResponse forecast, int index) {
        if (forecast == null || forecast.getDays() == null || index >= forecast.getDays().size()) {
            return null;
        }
        return forecast.getDays().get(index);
    }

    private static List<ForecastDay> weekendDays(ForecastResponse forecast) {
        if (forecast == null || forecast.getDays() == null) {
            return List.of();
        }
        return forecast.getDays().stream()
                .filter(day -> {
                    try {
                        DayOfWeek dayOfWeek = LocalDate.parse(day.getDate()).getDayOfWeek();
                        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }
}
