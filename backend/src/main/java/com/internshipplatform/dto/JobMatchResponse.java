package com.internshipplatform.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobMatchResponse {

    private Map<String, Object> profile;
    private int totalJobs;
    private List<JobMatch> matches;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobMatch {
        private int id;
        private String title;
        private int score;
        private String reason;
        private Map<String, String> fields;
    }
}
