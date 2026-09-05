package com.weathergpt.weather.provider;

import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastDay;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.dto.weather.LocationInfo;
import com.weathergpt.exception.WeatherProviderException;
import com.weathergpt.weather.WeatherProvider;
import com.weathergpt.weather.model.GeoLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Weather provider backed by the free Open-Meteo Forecast API
 * (https://open-meteo.com — no API key required).
 *
 * Raw provider JSON is normalized here into the stable WeatherGPT DTOs;
 * provider-specific field names and WMO weather codes never leak to clients.
 */
@Slf4j
@Service
public class OpenMeteoWeatherProvider implements WeatherProvider {

    private static final String PROVIDER_NAME = "Open-Meteo";

    private static final String CURRENT_FIELDS = "temperature_2m,apparent_temperature,"
            + "relative_humidity_2m,wind_speed_10m,wind_direction_10m,weather_code,"
            + "pressure_msl,visibility,is_day";

    private static final String DAILY_FIELDS = "weather_code,temperature_2m_max,temperature_2m_min,"
            + "precipitation_probability_max,precipitation_sum,wind_speed_10m_max,relative_humidity_2m_max";

    /** WMO weather interpretation codes -> human-readable description. */
    private static final Map<Integer, String> WMO_DESCRIPTIONS = Map.ofEntries(
            Map.entry(0, "Clear sky"),
            Map.entry(1, "Mainly clear"),
            Map.entry(2, "Partly cloudy"),
            Map.entry(3, "Overcast"),
            Map.entry(45, "Fog"),
            Map.entry(48, "Depositing rime fog"),
            Map.entry(51, "Light drizzle"),
            Map.entry(53, "Moderate drizzle"),
            Map.entry(55, "Dense drizzle"),
            Map.entry(56, "Light freezing drizzle"),
            Map.entry(57, "Dense freezing drizzle"),
            Map.entry(61, "Slight rain"),
            Map.entry(63, "Moderate rain"),
            Map.entry(65, "Heavy rain"),
            Map.entry(66, "Light freezing rain"),
            Map.entry(67, "Heavy freezing rain"),
            Map.entry(71, "Slight snowfall"),
            Map.entry(73, "Moderate snowfall"),
            Map.entry(75, "Heavy snowfall"),
            Map.entry(77, "Snow grains"),
            Map.entry(80, "Slight rain showers"),
            Map.entry(81, "Moderate rain showers"),
            Map.entry(82, "Violent rain showers"),
            Map.entry(85, "Slight snow showers"),
            Map.entry(86, "Heavy snow showers"),
            Map.entry(95, "Thunderstorm"),
            Map.entry(96, "Thunderstorm with slight hail"),
            Map.entry(99, "Thunderstorm with heavy hail")
    );

    private final RestTemplate restTemplate;

    @Value("${weather.api.base-url:https://api.open-meteo.com/v1}")
    private String baseUrl;

    public OpenMeteoWeatherProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CurrentWeatherResponse getCurrentWeather(GeoLocation location) {
        String url = baseUrl + "/forecast"
                + "?latitude=" + location.getLatitude()
                + "&longitude=" + location.getLongitude()
                + "&current=" + CURRENT_FIELDS
                + "&timezone=auto";

        try {
            Map<String, Object> raw = restTemplate.getForObject(url, Map.class);
            return normalizeCurrent(raw, location);
        } catch (RestClientException e) {
            log.error("Open-Meteo current weather request failed for {}: {}", location.getName(), e.getMessage());
            throw new WeatherProviderException(
                    "Weather service is temporarily unavailable. Please try again later.", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ForecastResponse getForecast(GeoLocation location, int forecastDays) {
        String url = baseUrl + "/forecast"
                + "?latitude=" + location.getLatitude()
                + "&longitude=" + location.getLongitude()
                + "&daily=" + DAILY_FIELDS
                + "&timezone=auto"
                + "&forecast_days=" + forecastDays;

        try {
            Map<String, Object> raw = restTemplate.getForObject(url, Map.class);
            return normalizeForecast(raw, location);
        } catch (RestClientException e) {
            log.error("Open-Meteo forecast request failed for {}: {}", location.getName(), e.getMessage());
            throw new WeatherProviderException(
                    "Weather service is temporarily unavailable. Please try again later.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private CurrentWeatherResponse normalizeCurrent(Map<String, Object> raw, GeoLocation location) {
        if (raw == null) {
            throw new WeatherProviderException("Weather service returned an empty response");
        }
        Map<String, Object> current = (Map<String, Object>) raw.get("current");
        if (current == null) {
            throw new WeatherProviderException("Weather service returned an invalid response");
        }

        Integer weatherCode = asInteger(current.get("weather_code"));

        return CurrentWeatherResponse.builder()
                .location(toLocationInfo(location, raw))
                .temperature(asDouble(current.get("temperature_2m")))
                .apparentTemperature(asDouble(current.get("apparent_temperature")))
                .humidity(asInteger(current.get("relative_humidity_2m")))
                .windSpeed(asDouble(current.get("wind_speed_10m")))
                .windDirection(asInteger(current.get("wind_direction_10m")))
                .weatherCode(weatherCode)
                .weatherDescription(describe(weatherCode))
                .pressure(asDouble(current.get("pressure_msl")))
                .visibility(asDouble(current.get("visibility")))
                .isDay(asBoolean(current.get("is_day")))
                .observedAt((String) current.get("time"))
                .timezone((String) raw.get("timezone"))
                .provider(PROVIDER_NAME)
                .build();
    }

    @SuppressWarnings("unchecked")
    private ForecastResponse normalizeForecast(Map<String, Object> raw, GeoLocation location) {
        if (raw == null) {
            throw new WeatherProviderException("Weather service returned an empty response");
        }
        Map<String, Object> daily = (Map<String, Object>) raw.get("daily");
        if (daily == null) {
            throw new WeatherProviderException("Weather service returned an invalid response");
        }

        List<String> dates = (List<String>) daily.get("time");
        List<Number> codes = (List<Number>) daily.get("weather_code");
        List<Number> tempMax = (List<Number>) daily.get("temperature_2m_max");
        List<Number> tempMin = (List<Number>) daily.get("temperature_2m_min");
        List<Number> precipProb = (List<Number>) daily.get("precipitation_probability_max");
        List<Number> precipSum = (List<Number>) daily.get("precipitation_sum");
        List<Number> windMax = (List<Number>) daily.get("wind_speed_10m_max");
        List<Number> humidityMax = (List<Number>) daily.get("relative_humidity_2m_max");

        List<ForecastDay> days = new ArrayList<>();
        if (dates != null) {
            for (int i = 0; i < dates.size(); i++) {
                Integer code = valueAt(codes, i) != null ? valueAt(codes, i).intValue() : null;
                days.add(ForecastDay.builder()
                        .date(dates.get(i))
                        .weatherCode(code)
                        .weatherDescription(describe(code))
                        .tempMax(asDouble(valueAt(tempMax, i)))
                        .tempMin(asDouble(valueAt(tempMin, i)))
                        .precipitationProbabilityMax(valueAt(precipProb, i) != null ? valueAt(precipProb, i).intValue() : null)
                        .precipitationSum(asDouble(valueAt(precipSum, i)))
                        .windSpeedMax(asDouble(valueAt(windMax, i)))
                        .humidityMax(valueAt(humidityMax, i) != null ? valueAt(humidityMax, i).intValue() : null)
                        .build());
            }
        }

        return ForecastResponse.builder()
                .location(toLocationInfo(location, raw))
                .timezone((String) raw.get("timezone"))
                .days(days)
                .provider(PROVIDER_NAME)
                .build();
    }

    private LocationInfo toLocationInfo(GeoLocation location, Map<String, Object> raw) {
        return LocationInfo.builder()
                .name(location.getName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .admin1(location.getAdmin1())
                .country(location.getCountry())
                .timezone((String) raw.get("timezone"))
                .build();
    }

    private static <T> T valueAt(List<T> list, int index) {
        if (list == null || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    private static String describe(Integer weatherCode) {
        if (weatherCode == null) {
            return null;
        }
        return WMO_DESCRIPTIONS.getOrDefault(weatherCode, "Unknown condition");
    }

    private static Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        return null;
    }
}
