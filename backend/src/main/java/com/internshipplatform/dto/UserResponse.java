package com.internshipplatform.dto;

import lombok.*;

import java.time.LocalDateTime;

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
    private String college;
    private String department;
    private String graduationYear;
    private String profilePictureUrl;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
