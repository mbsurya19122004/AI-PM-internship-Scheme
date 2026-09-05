package com.weathergpt;

import com.weathergpt.dto.alert.AlertResponse;
import com.weathergpt.dto.alert.WeatherAlertDto;
import com.weathergpt.exception.ResourceNotFoundException;
import com.weathergpt.service.AlertService;
import com.weathergpt.weather.GeocodingProvider;
import com.weathergpt.weather.alert.AlertInformationClass;
import com.weathergpt.weather.alert.AlertSeverity;
import com.weathergpt.weather.alert.AlertType;
import com.weathergpt.weather.alert.WeatherAlertProvider;
import com.weathergpt.weather.model.GeoLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for AlertService.
 * Verifies alert aggregation, provider classification enforcement,
 * and provider failure handling.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private GeocodingProvider geocodingProvider;

    @Mock
    private WeatherAlertProvider alertProvider;

    @InjectMocks
    private AlertService alertService;

    private static final GeoLocation DELHI = GeoLocation.builder()
            .name("Delhi")
            .latitude(28.6519)
            .longitude(77.2315)
            .admin1("Delhi")
            .country("India")
            .timezone("Asia/Kolkata")
            .build();

    @BeforeEach
    void setUp() {
        lenient().when(geocodingProvider.resolve("Delhi")).thenReturn(Optional.of(DELHI));
    }

    @Test
    @DisplayName("Returns empty alert list when no-op provider returns no alerts")
    void getAlerts_noProviderAlerts_returnsEmptyList() {
        given(alertProvider.isOfficialSource()).willReturn(false);
        given(alertProvider.getAlerts(DELHI)).willReturn(List.of());

        AlertResponse response = alertService.getAlerts("Delhi");

        assertThat(response).isNotNull();
        assertThat(response.getAlerts()).isEmpty();
        assertThat(response.getTotalAlerts()).isZero();
        assertThat(response.isOfficialProviderActive()).isFalse();
        assertThat(response.getLocation()).isEqualTo("Delhi");
    }

    @Test
    @DisplayName("Returns alerts from provider with correct metadata")
    void getAlerts_withProviderAlerts_returnsAlerts() {
        WeatherAlertDto alert = WeatherAlertDto.builder()
                .id("alert-001")
                .title("Heavy Rain Advisory")
                .description("Heavy rainfall expected over Delhi.")
                .informationClass(AlertInformationClass.AUTOMATED_ADVISORY)
                .alertType(AlertType.HEAVY_RAIN)
                .severity(AlertSeverity.MODERATE)
                .source("WeatherGPT Automated Advisory System")
                .official(false)
                .issuedAt(Instant.now())
                .build();

        given(alertProvider.isOfficialSource()).willReturn(false);
        given(alertProvider.getAlerts(DELHI)).willReturn(List.of(alert));

        AlertResponse response = alertService.getAlerts("Delhi");

        assertThat(response.getAlerts()).hasSize(1);
        assertThat(response.getTotalAlerts()).isEqualTo(1);
        WeatherAlertDto returnedAlert = response.getAlerts().get(0);
        assertThat(returnedAlert.getInformationClass()).isEqualTo(AlertInformationClass.AUTOMATED_ADVISORY);
        assertThat(returnedAlert.isOfficial()).isFalse();
    }

    @Test
    @DisplayName("Official provider sets officialProviderActive = true")
    void getAlerts_officialProvider_setsOfficialFlagTrue() {
        given(alertProvider.isOfficialSource()).willReturn(true);
        given(alertProvider.getAlerts(DELHI)).willReturn(List.of());

        AlertResponse response = alertService.getAlerts("Delhi");

        assertThat(response.isOfficialProviderActive()).isTrue();
    }

    @Test
    @DisplayName("Non-official provider producing OFFICIAL_WARNING is reclassified to AUTOMATED_ADVISORY")
    void getAlerts_nonOfficialProviderWithOfficialWarning_reclassifiesAlert() {
        // Simulate a badly-behaved provider that marks a non-official alert as official
        WeatherAlertDto badAlert = WeatherAlertDto.builder()
                .id("bad-001")
                .title("Should not be official")
                .description("This was incorrectly marked as official.")
                .informationClass(AlertInformationClass.OFFICIAL_WARNING)
                .alertType(AlertType.THUNDERSTORM)
                .severity(AlertSeverity.SEVERE)
                .source("Untrusted Source")
                .official(true)
                .build();

        given(alertProvider.isOfficialSource()).willReturn(false);
        given(alertProvider.getProviderName()).willReturn("BadProvider");
        given(alertProvider.getAlerts(DELHI)).willReturn(List.of(badAlert));

        AlertResponse response = alertService.getAlerts("Delhi");

        assertThat(response.getAlerts()).hasSize(1);
        WeatherAlertDto reclassified = response.getAlerts().get(0);
        // Must be reclassified from OFFICIAL_WARNING to AUTOMATED_ADVISORY
        assertThat(reclassified.getInformationClass()).isEqualTo(AlertInformationClass.AUTOMATED_ADVISORY);
        assertThat(reclassified.isOfficial()).isFalse();
    }

    @Test
    @DisplayName("Returns error status when provider throws an exception")
    void getAlerts_providerThrowsException_returnsErrorStatus() {
        given(alertProvider.isOfficialSource()).willReturn(false);
        given(alertProvider.getProviderName()).willReturn("TestProvider");
        given(alertProvider.getAlerts(DELHI)).willThrow(
                new com.weathergpt.exception.WeatherProviderException("Provider unavailable"));

        AlertResponse response = alertService.getAlerts("Delhi");

        assertThat(response.getAlerts()).isEmpty();
        assertThat(response.getProviderStatus()).contains("unavailable");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when location cannot be resolved")
    void getAlerts_unknownLocation_throwsResourceNotFoundException() {
        given(geocodingProvider.resolve("UnknownCityXYZ")).willReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.getAlerts("UnknownCityXYZ"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Location not found: UnknownCityXYZ");
    }

    @Test
    @DisplayName("Response includes latitude and longitude from resolved location")
    void getAlerts_includesLocationCoordinates() {
        given(alertProvider.isOfficialSource()).willReturn(false);
        given(alertProvider.getAlerts(DELHI)).willReturn(List.of());

        AlertResponse response = alertService.getAlerts("Delhi");

        assertThat(response.getLatitude()).isEqualTo(DELHI.getLatitude());
        assertThat(response.getLongitude()).isEqualTo(DELHI.getLongitude());
    }

    @Test
    @DisplayName("Provider status message mentions official sources when no official provider is active")
    void getAlerts_noOfficialProvider_statusMentionsOfficialSources() {
        given(alertProvider.isOfficialSource()).willReturn(false);
        given(alertProvider.getAlerts(DELHI)).willReturn(List.of());

        AlertResponse response = alertService.getAlerts("Delhi");

        assertThat(response.getProviderStatus()).containsIgnoringCase("pending");
        assertThat(response.isOfficialProviderActive()).isFalse();
    }
}
