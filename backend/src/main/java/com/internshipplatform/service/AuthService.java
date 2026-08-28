package com.internshipplatform.service;

import com.internshipplatform.dto.*;
import com.internshipplatform.entity.User;
import com.internshipplatform.repository.UserRepository;
import com.internshipplatform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .college(request.getCollege())
                .department(request.getDepartment())
                .graduationYear(request.getGraduationYear())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        UserResponse userResponse = mapToUserResponse(user);

        return AuthResponse.of(accessToken, refreshToken, tokenProvider.getJwtExpiration(), userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        UserResponse userResponse = mapToUserResponse(user);

        log.info("User logged in: {}", request.getEmail());

        return AuthResponse.of(accessToken, refreshToken, tokenProvider.getJwtExpiration(), userResponse);
    }

    public AuthResponse refreshTokens(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Token is not a refresh token");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String newAccessToken = tokenProvider.generateAccessTokenFromEmail(email);
        String newRefreshToken = tokenProvider.generateRefreshTokenFromEmail(email);

        UserResponse userResponse = mapToUserResponse(user);

        log.info("Tokens refreshed for user: {}", email);

        return AuthResponse.of(newAccessToken, newRefreshToken, tokenProvider.getJwtExpiration(), userResponse);
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .college(user.getCollege())
                .department(user.getDepartment())
                .graduationYear(user.getGraduationYear())
                .profilePictureUrl(user.getProfilePictureUrl())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
