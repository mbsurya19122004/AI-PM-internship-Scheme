package com.weathergpt.controller;

import com.weathergpt.dto.ApiResponse;
import com.weathergpt.dto.UserResponse;
import com.weathergpt.entity.User;
import com.weathergpt.repository.UserRepository;
import com.weathergpt.security.SecurityEventLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin-only management endpoints.
 * Intended for system administration: user management, role assignment, and
 * future operational monitoring (weather providers, alert sources, etc.).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final SecurityEventLogger securityEventLogger;

    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        String newRole = request.get("role");
        if (newRole == null || newRole.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Role is required"));
        }

        if (!ALLOWED_ROLES.contains(newRole)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid role. Allowed roles: ROLE_USER, ROLE_ADMIN"));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        String ipAddress = httpRequest.getRemoteAddr();
        securityEventLogger.logUserRoleChanged(user.getEmail(), oldRole, newRole, ipAddress);

        return ResponseEntity.ok(ApiResponse.success("User role updated successfully"));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
