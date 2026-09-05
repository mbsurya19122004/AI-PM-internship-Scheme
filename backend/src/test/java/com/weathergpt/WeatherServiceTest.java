package com.weathergpt;

import com.weathergpt.dto.weather.CurrentWeatherResponse;
import com.weathergpt.dto.weather.ForecastResponse;
import com.weathergpt.exception.ResourceNotFoundException;
import com.weathergpt.exception.WeatherProviderException;
import com.weathergpt.service.WeatherService;
import com.weathergpt.weather.GeocodingProvider;
import com.weathergpt.weather.WeatherProvider;
import com.weathergpt.weather.model.GeoLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private GeocodingProvider geocodingProvider;

    @Mock
    private WeatherProvider weatherProvider;

    @InjectMocks
    private WeatherService weatherService;

    private static final GeoLocation DELHI = GeoLocation.builder()
            .name("Delhi")
            .latitude(28.65195)
            .longitude(77.23149)
            .timezone("Asia/Kolkata")
            .build();

    @Test
    @DisplayName("Current weather uses resolved coordinates and returns provider response")
    void getCurrentWeatherUsesResolvedLocation() {
        when(geocodingProvider.resolve("Delhi")).thenReturn(Optional.of(DELHI));
        CurrentWeatherResponse expected = CurrentWeatherResponse.builder().temperature(27.9).build();
        when(weatherProvider.getCurrentWeather(DELHI)).thenReturn(expected);

        CurrentWeatherResponse result = weatherService.getCurrentWeather("Delhi");

        assertSame(expected, result);
        // Coordinates must be passed to the provider as a normalized location
        verify(weatherProvider).getCurrentWeather(DELHI);
    }

    @Test
    @DisplayName("Forecast uses resolved coordinates and passes requested days")
    void getForecastUsesResolvedLocation() {
        when(geocodingProvider.resolve("Mumbai")).thenReturn(Optional.of(DELHI));
        ForecastResponse expected = ForecastResponse.builder().build();
        when(weatherProvider.getForecast(DELHI, 7)).thenReturn(expected);

        ForecastResponse result = weatherService.getForecast("Mumbai", 7);

        assertSame(expected, result);
        verify(weatherProvider).getForecast(DELHI, 7);
    }

    @Test
    @DisplayName("Unknown location throws ResourceNotFoundException and never calls weather provider")
    void unknownLocationThrowsNotFound() {
        when(geocodingProvider.resolve("InvalidFakeLocationXYZ")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> weatherService.getCurrentWeather("InvalidFakeLocationXYZ"));

        verify(weatherProvider, never()).getCurrentWeather(any());
        verify(weatherProvider, never()).getForecast(any(), anyInt());
    }

    @Test
    @DisplayName("Provider failure propagates as WeatherProviderException")
    void providerFailurePropagates() {
        when(geocodingProvider.resolve("Delhi")).thenReturn(Optional.of(DELHI));
        when(weatherProvider.getCurrentWeather(DELHI))
                .thenThrow(new WeatherProviderException("Weather service is temporarily unavailable"));

        assertThrows(WeatherProviderException.class, () -> weatherService.getCurrentWeather("Delhi"));
    }

    @Test
    @DisplayName("resolveLocation returns the geocoded location")
    void resolveLocationReturnsGeoLocation() {
        when(geocodingProvider.resolve("Delhi")).thenReturn(Optional.of(DELHI));

        assertSame(DELHI, weatherService.resolveLocation("Delhi"));
    }

    @Test
    @DisplayName("resolveLocation throws ResourceNotFoundException for unknown places")
    void resolveLocationUnknownThrowsNotFound() {
        when(geocodingProvider.resolve("FakeCityXYZ")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> weatherService.resolveLocation("FakeCityXYZ"));
    }

    @Test
    @DisplayName("Current weather by GeoLocation delegates directly to the provider without re-geocoding")
    void getCurrentWeatherByGeoLocationDelegatesToProvider() {
        CurrentWeatherResponse expected = CurrentWeatherResponse.builder().temperature(27.9).build();
        when(weatherProvider.getCurrentWeather(DELHI)).thenReturn(expected);

        assertSame(expected, weatherService.getCurrentWeather(DELHI));

        verify(geocodingProvider, never()).resolve(anyString());
    }

    @Test
    @DisplayName("Forecast by GeoLocation delegates directly to the provider without re-geocoding")
    void getForecastByGeoLocationDelegatesToProvider() {
        ForecastResponse expected = ForecastResponse.builder().build();
        when(weatherProvider.getForecast(DELHI, 7)).thenReturn(expected);

        assertSame(expected, weatherService.getForecast(DELHI, 7));

        verify(geocodingProvider, never()).resolve(anyString());
    }
}