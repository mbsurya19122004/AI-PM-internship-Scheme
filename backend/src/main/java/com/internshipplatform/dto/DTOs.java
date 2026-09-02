package com.internshipplatform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Consolidated DTOs (Data Transfer Objects) for the Internship Platform.
 * All DTO classes are nested static inner classes for organizational clarity.
 */
public final class DTOs {

    private DTOs() {
        // Utility class - no instantiation
    }

    // ==================== API RESPONSE ====================

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(String message, T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> success(String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }

    // ==================== AUTH DTOs ====================

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Please provide a valid phone number")
        private String phoneNumber;

        @NotBlank(message = "College name is required")
        private String college;

        private String department;

        private String graduationYear;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
        )
        private String password;

        @NotBlank(message = "Password confirmation is required")
        private String confirmPassword;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
        private UserResponse user;

        public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(expiresIn)
                    .user(user)
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
        )
        private String newPassword;

        @NotBlank(message = "Password confirmation is required")
        private String confirmNewPassword;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResetPasswordRequest {
        @NotBlank(message = "Reset token is required")
        private String token;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
        )
        private String newPassword;

        @NotBlank(message = "Password confirmation is required")
        private String confirmNewPassword;
    }

    // ==================== USER DTOs ====================

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserResponse {
        private Long id;
        private String email;
        private String fullName;
        private String phoneNumber;
        private String college;
        private String department;
        private String graduationYear;
        private String profilePictureUrl;
        private boolean enabled;
        private boolean emailVerified;
        private LocalDateTime createdAt;
    }

    // ==================== INTERNSHIP DTOs ====================

    public static class InternshipDto {

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
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

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
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

    // ==================== RESUME DTOs ====================

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResumeResponse {
        private Long id;
        private String fileName;
        private String originalFileName;
        private String contentType;
        private Long fileSize;
        private String description;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ==================== ML DTOs ====================

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalyzeResumeRequest {
        @NotBlank(message = "Resume text is required")
        private String resumeText;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobMatchResponse {
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
}
