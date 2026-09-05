package com.weathergpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weathergpt.dto.alert.AlertResponse;
import com.weathergpt.dto.alert.WeatherAlertDto;
import com.weathergpt.service.AlertService;
import com.weathergpt.weather.alert.AlertInformationClass;
import com.weathergpt.weather.alert.AlertSeverity;
import com.weathergpt.weather.alert.AlertType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AlertController.
 * Verifies endpoint behavior, response structure, and informationClass distinction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_alert",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ="
})
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @Test
    @DisplayName("GET /api/alerts without location returns 400")
    void getAlerts_noLocation_returns400() throws Exception {
        mockMvc.perform(get("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/alerts with valid location and no active alerts returns 200 with empty list")
    void getAlerts_validLocation_noAlerts_returns200() throws Exception {
        AlertResponse emptyResponse = AlertResponse.builder()
                .location("Delhi")
                .latitude(28.6519)
                .longitude(77.2315)
                .alerts(List.of())
                .totalAlerts(0)
                .officialProviderActive(false)
                .providerStatus("Official extreme weather alert integration is pending.")
                .build();

        given(alertService.getAlerts("Delhi")).willReturn(emptyResponse);

        mockMvc.perform(get("/api/alerts")
                        .param("location", "Delhi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.location").value("Delhi"))
                .andExpect(jsonPath("$.data.totalAlerts").value(0))
                .andExpect(jsonPath("$.data.alerts").isArray())
                .andExpect(jsonPath("$.data.officialProviderActive").value(false))
                .andExpect(jsonPath("$.data.providerStatus").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/alerts returns alert with informationClass field")
    void getAlerts_withAlert_includesInformationClass() throws Exception {
        WeatherAlertDto alert = WeatherAlertDto.builder()
                .id("test-001")
                .title("Heavy Rain Advisory")
                .description("Heavy rainfall expected over Mumbai.")
                .informationClass(AlertInformationClass.AUTOMATED_ADVISORY)
                .alertType(AlertType.HEAVY_RAIN)
                .severity(AlertSeverity.MODERATE)
                .source("WeatherGPT Automated Advisory System")
                .official(false)
                .issuedAt(Instant.now())
                .build();

        AlertResponse response = AlertResponse.builder()
                .location("Mumbai")
                .latitude(19.0728)
                .longitude(72.8826)
                .alerts(List.of(alert))
                .totalAlerts(1)
                .officialProviderActive(false)
                .providerStatus("Official extreme weather alert integration is pending.")
                .build();

        given(alertService.getAlerts("Mumbai")).willReturn(response);

        mockMvc.perform(get("/api/alerts")
                        .param("location", "Mumbai")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAlerts").value(1))
                .andExpect(jsonPath("$.data.alerts[0].informationClass").value("AUTOMATED_ADVISORY"))
                .andExpect(jsonPath("$.data.alerts[0].official").value(false))
                .andExpect(jsonPath("$.data.alerts[0].severity").value("MODERATE"))
                .andExpect(jsonPath("$.data.alerts[0].alertType").value("HEAVY_RAIN"))
                .andExpect(jsonPath("$.data.alerts[0].source").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/alerts is publicly accessible without authentication")
    void getAlerts_isPublicEndpoint() throws Exception {
        AlertResponse emptyResponse = AlertResponse.builder()
                .location("Chennai")
                .alerts(List.of())
                .totalAlerts(0)
                .officialProviderActive(false)
                .providerStatus("Pending integration.")
                .build();

        given(alertService.getAlerts("Chennai")).willReturn(emptyResponse);

        // No Authorization header - should still return 200
        mockMvc.perform(get("/api/alerts")
                        .param("location", "Chennai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/alerts when location not found returns 404")
    void getAlerts_unknownLocation_returns404() throws Exception {
        given(alertService.getAlerts("UnknownCityXYZ"))
                .willThrow(new com.weathergpt.exception.ResourceNotFoundException("Location not found: UnknownCityXYZ"));

        mockMvc.perform(get("/api/alerts")
                        .param("location", "UnknownCityXYZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
