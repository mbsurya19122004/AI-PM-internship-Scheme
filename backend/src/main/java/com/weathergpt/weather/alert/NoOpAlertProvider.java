package com.weathergpt.weather.alert;

import com.weathergpt.dto.alert.WeatherAlertDto;
import com.weathergpt.weather.model.GeoLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default alert provider used when no official alert integration is configured.
 *
 * This provider truthfully reports that official alert integration is pending.
 * It returns an empty alert list and is marked as NOT an official source.
 *
 * Replace with a real implementation (e.g., ImdAlertProvider, CapFeedAlertProvider)
 * when an actual official data source becomes available.
 *
 * IMPORTANT: This class must NOT fabricate official warnings.
 * It exists solely to allow the alert architecture to function without a live feed.
 *
 * Activation: This bean is only registered when no other {@link WeatherAlertProvider}
 * bean is present in the application context.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(name = "officialAlertProvider")
public class NoOpAlertProvider implements WeatherAlertProvider {

    private static final String PROVIDER_NAME = "No Official Alert Provider (Pending Integration)";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isOfficialSource() {
        // This is NOT an official source — it is a placeholder.
        return false;
    }

    @Override
    public List<WeatherAlertDto> getAlerts(GeoLocation location) {
        log.debug("NoOpAlertProvider: No official alert provider configured. "
                + "Returning empty alert list for location: {}", location.getName());
        // Return empty list — do NOT fabricate alerts.
        return List.of();
    }
}
