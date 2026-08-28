package com.internshipplatform.controller;

import com.internshipplatform.dto.ApiResponse;
import com.internshipplatform.dto.JobMatchResponse;
import com.internshipplatform.service.MlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MlController {

    private final MlService mlService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        boolean available = mlService.isAvailable();
        return ResponseEntity.ok(ApiResponse.success(
                available ? "ML service is available" : "ML service is unavailable",
                Map.of("available", available)
        ));
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeResume(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        String resumeText = request.get("resumeText");
        if (resumeText == null || resumeText.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("resumeText is required"));
        }
        Map<String, Object> profile = mlService.analyzeResume(resumeText);
        return ResponseEntity.ok(ApiResponse.success("Resume analyzed successfully", profile));
    }

    @PostMapping("/match")
    public ResponseEntity<ApiResponse<JobMatchResponse>> matchJobs(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "topN", defaultValue = "10") int topN) {
        JobMatchResponse response = mlService.matchJobs(file, topN);
        return ResponseEntity.ok(ApiResponse.success("Jobs matched successfully", response));
    }
}
