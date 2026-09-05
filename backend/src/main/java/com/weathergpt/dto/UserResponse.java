package com.weathergpt.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Public user profile response. Contains only WeatherGPT-relevant fields.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
