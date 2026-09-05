package com.weathergpt;

import com.weathergpt.dto.alert.WeatherAlertDto;
import com.weathergpt.weather.alert.NoOpAlertProvider;
import com.weathergpt.weather.model.GeoLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for NoOpAlertProvider.
 * Verifies that the placeholder implementation:
 * 1. Never fabricates alerts.
 * 2. Correctly identifies itself as non-official.
 */
class NoOpAlertProviderTest {

    private final NoOpAlertProvider provider = new NoOpAlertProvider();

    private static final GeoLocation DELHI = GeoLocation.builder()
            .name("Delhi").latitude(28.6519).longitude(77.2315)
            .country("India").timezone("Asia/Kolkata").build();

    @Test
    @DisplayName("isOfficialSource returns false")
    void isOfficialSource_returnsFalse() {
        assertThat(provider.isOfficialSource()).isFalse();
    }

    @Test
    @DisplayName("getAlerts returns empty list (no fabricated alerts)")
    void getAlerts_returnsEmptyList() {
        List<WeatherAlertDto> alerts = provider.getAlerts(DELHI);
        assertThat(alerts).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("getAlerts never throws exception")
    void getAlerts_neverThrows() {
        assertThatCode(() -> provider.getAlerts(DELHI)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getProviderName returns a non-blank string")
    void getProviderName_returnsNonBlank() {
        assertThat(provider.getProviderName()).isNotBlank();
    }
}
