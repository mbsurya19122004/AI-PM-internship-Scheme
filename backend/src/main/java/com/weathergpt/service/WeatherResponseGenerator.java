package com.weathergpt.service;

import com.weathergpt.dto.chat.ChatResponse;
import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastDay;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.dto.weather.LocationInfo;
import com.weathergpt.weather.model.GeoLocation;
import com.weathergpt.weather.query.ParsedWeatherQuery;
import com.weathergpt.weather.query.TimeReference;
import com.weathergpt.weather.query.WeatherAspect;
import com.weathergpt.weather.query.WeatherIntent;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Builds conversational answers and simple data-driven advisories strictly from
 * normalized weather data. The generator never invents conditions: every claim
 * is derived from the retrieved weather payload.
 *
 * Advisories here are GENERAL WEATHER ADVISORIES derived from data thresholds —
 * never official weather alerts or warnings.
 */
@Service
public class WeatherResponseGenerator {

    private static final double UMBRELLA_PRECIPITATION_THRESHOLD = 50.0;
    private static final double HEAT_TEMPERATURE_THRESHOLD = 40.0;
    private static final double WIND_SPEED_THRESHOLD = 40.0;

    /** Answer based on real-time conditions. */
    public ChatResponse currentWeather(ParsedWeatherQuery query, GeoLocation location, CurrentWeatherResponse current) {
        String core = currentAnswerCore(location, current, query.getAspect());
        List<String> advisories = advisories(
                current.getTemperature(),
                null,
                current.getWindSpeed());
        return base(query, location, current, null, core, advisories);
    }

    /** Answer based on a single forecast day (tomorrow / today for rain queries). */
    public ChatResponse forecastDay(ParsedWeatherQuery query, GeoLocation location, ForecastResponse forecast,
                                    ForecastDay day, TimeReference time) {
        String core = forecastDayAnswerCore(query.getIntent(), location, day, time);
        List<String> advisories = advisories(
                day != null ? day.getTempMax() : null,
                day != null ? day.getPrecipitationProbabilityMax() : null,
                day != null ? day.getWindSpeedMax() : null);
        return base(query, location, null, forecast, core, advisories);
    }

    /** Answer based on the full 7-day forecast (rain-oriented or general). */
    public ChatResponse forecastWeek(ParsedWeatherQuery query, GeoLocation location, ForecastResponse forecast) {
        String core = weekAnswerCore(query.getIntent(), location, forecast);
        List<String> advisories = weekAdvisories(forecast);
        return base(query, location, null, forecast, core, advisories);
    }

    /** Answer based on the weekend days within the forecast window. */
    public ChatResponse forecastWeekend(ParsedWeatherQuery query, GeoLocation location, List<ForecastDay> weekendDays) {
        String core = weekendAnswerCore(query.getIntent(), location, weekendDays);
        List<String> advisories = daysAdvisories(weekendDays);
        return base(query, location, null, null, core, advisories);
    }

    private String currentAnswerCore(GeoLocation location, CurrentWeatherResponse current, WeatherAspect aspect) {
        String name = location.getName();
        return switch (aspect) {
            case TEMPERATURE -> "It is currently " + fmt(current.getTemperature()) + "°C in " + name
                    + (current.getApparentTemperature() != null
                            ? " (feels like " + fmt(current.getApparentTemperature()) + "°C)" : "")
                    + ".";
            case WIND -> "The wind in " + name + " is currently " + fmt(current.getWindSpeed()) + " km/h"
                    + (current.getWindDirection() != null
                            ? " from direction " + current.getWindDirection() + "°" : "")
                    + ".";
            case HUMIDITY -> "The humidity in " + name + " is currently "
                    + (current.getHumidity() != null ? current.getHumidity() : "n/a") + "%.";
            default -> {
                StringBuilder sb = new StringBuilder("The current weather in ").append(name).append(" is ");
                sb.append(lower(current.getWeatherDescription()));
                sb.append(" with a temperature of ").append(fmt(current.getTemperature())).append("°C");
                if (current.getHumidity() != null) {
                    sb.append(", humidity ").append(current.getHumidity()).append("%");
                }
                if (current.getWindSpeed() != null) {
                    sb.append(" and wind ").append(fmt(current.getWindSpeed())).append(" km/h");
                }
                sb.append(".");
                yield sb.toString();
            }
        };
    }

    private String forecastDayAnswerCore(WeatherIntent intent, GeoLocation location, ForecastDay day, TimeReference time) {
        if (day == null) {
            return "Forecast data is currently unavailable for " + location.getName() + ".";
        }
        String timeLabel = timeLabel(time);
        Integer precipitation = day.getPrecipitationProbabilityMax();
        return switch (intent) {
            case RAIN_QUERY -> {
                if (precipitation != null) {
                    yield precipitation >= UMBRELLA_PRECIPITATION_THRESHOLD
                            ? "Yes. Rain is likely " + timeLabel + " in " + location.getName()
                                    + " with a " + precipitation + "% chance of precipitation."
                            : "Rain is unlikely " + timeLabel + " in " + location.getName()
                                    + " (precipitation chance " + precipitation + "%).";
                }
                yield "The weather " + timeLabel + " in " + location.getName() + " is expected to be "
                        + lower(day.getWeatherDescription()) + ".";
            }
            case TEMPERATURE_QUERY -> "The temperature " + timeLabel + " in " + location.getName()
                    + " will reach " + fmt(day.getTempMax()) + "°C with a low of " + fmt(day.getTempMin()) + "°C"
                    + (day.getWeatherDescription() != null ? " and " + lower(day.getWeatherDescription()) : "")
                    + ".";
            default -> "The weather " + timeLabel + " in " + location.getName() + " is expected to be "
                    + lower(day.getWeatherDescription()) + " with highs of " + fmt(day.getTempMax())
                    + "°C and lows of " + fmt(day.getTempMin()) + "°C.";
        };
    }

    private String weekAnswerCore(WeatherIntent intent, GeoLocation location, ForecastResponse forecast) {
        if (forecast == null || forecast.getDays() == null || forecast.getDays().isEmpty()) {
            return "Forecast data is currently unavailable for " + location.getName() + ".";
        }
        if (intent == WeatherIntent.RAIN_QUERY) {
            List<ForecastDay> rainy = forecast.getDays().stream()
                    .filter(d -> d.getPrecipitationProbabilityMax() != null
                            && d.getPrecipitationProbabilityMax() >= UMBRELLA_PRECIPITATION_THRESHOLD)
                    .toList();
            if (rainy.isEmpty()) {
                return "No significant rain is expected this week in " + location.getName() + ".";
            }
            return "Rain is possible this week in " + location.getName() + " on: "
                    + rainy.stream().map(ForecastDay::getDate).collect(Collectors.joining(", ")) + ".";
        }
        String summary = forecast.getDays().stream()
                .map(this::formatDayShort)
                .collect(Collectors.joining("; "));
        return "The forecast for " + location.getName() + " this week is: " + summary + ".";
    }

    private String weekendAnswerCore(WeatherIntent intent, GeoLocation location, List<ForecastDay> weekendDays) {
        if (weekendDays == null || weekendDays.isEmpty()) {
            return "I don't have weekend forecast data for " + location.getName() + ".";
        }
        String summary = weekendDays.stream()
                .map(d -> dayName(d.getDate()) + ": " + formatDayShort(d))
                .collect(Collectors.joining("; "));
        return "This weekend in " + location.getName() + ": " + summary + ".";
    }

    private ChatResponse base(ParsedWeatherQuery query, GeoLocation location, CurrentWeatherResponse current,
                              ForecastResponse forecast, String core, List<String> advisories) {
        String answer = advisories.isEmpty() ? core : core + " " + String.join(" ", advisories);
        return ChatResponse.builder()
                .answer(answer)
                .intent(query.getIntent())
                .timeReference(query.getTimeReference())
                .location(toLocationInfo(location))
                .currentWeather(current)
                .forecast(forecast)
                .advisories(advisories)
                .build();
    }

    private List<String> advisories(Double temperature, Integer precipitationProbability, Double windSpeed) {
        List<String> list = new ArrayList<>();
        if (precipitationProbability != null && precipitationProbability >= UMBRELLA_PRECIPITATION_THRESHOLD) {
            list.add("Consider carrying an umbrella.");
        }
        if (temperature != null && temperature >= HEAT_TEMPERATURE_THRESHOLD) {
            list.add("Very hot conditions. Stay hydrated and avoid prolonged sun exposure.");
        }
        if (windSpeed != null && windSpeed >= WIND_SPEED_THRESHOLD) {
            list.add("Strong winds expected. Exercise caution outdoors.");
        }
        return list;
    }

    private List<String> weekAdvisories(ForecastResponse forecast) {
        if (forecast == null || forecast.getDays() == null) {
            return List.of();
        }
        Double maxTemp = forecast.getDays().stream()
                .map(ForecastDay::getTempMax)
                .filter(java.util.Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);
        Double maxWind = forecast.getDays().stream()
                .map(ForecastDay::getWindSpeedMax)
                .filter(java.util.Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);
        Integer maxPrecip = forecast.getDays().stream()
                .map(ForecastDay::getPrecipitationProbabilityMax)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);
        return advisories(maxTemp, maxPrecip, maxWind);
    }

    private List<String> daysAdvisories(List<ForecastDay> days) {
        if (days == null || days.isEmpty()) {
            return List.of();
        }
        Double maxTemp = days.stream().map(ForecastDay::getTempMax)
                .filter(java.util.Objects::nonNull).max(Double::compareTo).orElse(null);
        Double maxWind = days.stream().map(ForecastDay::getWindSpeedMax)
                .filter(java.util.Objects::nonNull).max(Double::compareTo).orElse(null);
        Integer maxPrecip = days.stream().map(ForecastDay::getPrecipitationProbabilityMax)
                .filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(null);
        return advisories(maxTemp, maxPrecip, maxWind);
    }

    private String formatDayShort(ForecastDay day) {
        return day.getDate() + ": " + lower(day.getWeatherDescription())
                + ", " + fmt(day.getTempMax()) + "°C/" + fmt(day.getTempMin()) + "°C";
    }

    private static String dayName(String date) {
        try {
            return LocalDate.parse(date).getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } catch (Exception e) {
            return date;
        }
    }

    private static String timeLabel(TimeReference time) {
        return switch (time) {
            case TOMORROW, NEXT_DAY -> "tomorrow";
            case THIS_WEEK -> "this week";
            case THIS_WEEKEND -> "this weekend";
            default -> "today";
        };
    }

    private static LocationInfo toLocationInfo(GeoLocation location) {
        return LocationInfo.builder()
                .name(location.getName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .admin1(location.getAdmin1())
                .country(location.getCountry())
                .timezone(location.getTimezone())
                .build();
    }

    private static String lower(String text) {
        if (text == null || text.isBlank()) {
            return "unknown conditions";
        }
        return text.substring(0, 1).toLowerCase() + text.substring(1);
    }

    private static String fmt(Double value) {
        if (value == null) {
            return "n/a";
        }
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf(value.longValue());
        }
        return String.valueOf(value);
    }
}
