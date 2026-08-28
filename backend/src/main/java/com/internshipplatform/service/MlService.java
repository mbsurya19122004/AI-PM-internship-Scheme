package com.internshipplatform.service;

import com.internshipplatform.dto.JobMatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
public class MlService {

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeResume(String resumeText) {
        String url = mlServiceUrl + "/analyze-resume";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("resumeText", resumeText);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (response.getBody() != null && (boolean) response.getBody().getOrDefault("success", false)) {
                return (Map<String, Object>) response.getBody().get("data");
            }
            throw new RuntimeException("ML service returned unsuccessful response");
        } catch (Exception e) {
            log.error("Failed to analyze resume: {}", e.getMessage());
            throw new RuntimeException("ML service unavailable: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public JobMatchResponse matchJobs(MultipartFile file, int topN) {
        String url = mlServiceUrl + "/match-jobs-file?top_n=" + topN;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (response.getBody() != null && (boolean) response.getBody().getOrDefault("success", false)) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");

                JobMatchResponse.JobMatchResponseBuilder builder = JobMatchResponse.builder();

                if (data.containsKey("profile")) {
                    builder.profile((Map<String, Object>) data.get("profile"));
                }
                builder.totalJobs(((Number) data.getOrDefault("totalJobs", 0)).intValue());

                var matches = (java.util.List<Map<String, Object>>) data.getOrDefault("matches", java.util.List.of());
                var jobMatches = matches.stream()
                        .map(m -> JobMatchResponse.JobMatch.builder()
                                .id(((Number) m.getOrDefault("id", 0)).intValue())
                                .title((String) m.getOrDefault("title", ""))
                                .score(((Number) m.getOrDefault("score", 0)).intValue())
                                .reason((String) m.getOrDefault("reason", ""))
                                .fields((Map<String, String>) m.getOrDefault("fields", Map.of()))
                                .build())
                        .toList();
                builder.matches(jobMatches);

                return builder.build();
            }
            throw new RuntimeException("ML service returned unsuccessful response");
        } catch (Exception e) {
            log.error("Failed to match jobs: {}", e.getMessage());
            throw new RuntimeException("ML service unavailable: " + e.getMessage());
        }
    }

    public boolean isAvailable() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(mlServiceUrl + "/health", Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
