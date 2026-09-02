package com.internshipplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Request and response DTOs for Internship API — combined in one file to minimize file count.
 */
public class InternshipDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Request {
        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
        private String title;

        @NotBlank(message = "Company name is required")
        @Size(min = 1, max = 200, message = "Company name must be between 1 and 200 characters")
        private String company;

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        private String description;

        @NotBlank(message = "Application link is required")
        private String applicationLink;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Response {
        private Long id;
        private String title;
        private String company;
        private String description;
        private String applicationLink;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
