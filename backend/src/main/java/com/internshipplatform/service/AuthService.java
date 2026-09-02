package com.internshipplatform.service;

import com.internshipplatform.dto.*;
import com.internshipplatform.entity.EmailVerificationToken;
import com.internshipplatform.entity.PasswordResetToken;
import com.internshipplatform.entity.User;
import com.internshipplatform.repository.EmailVerificationTokenRepository;
import com.internshipplatform.repository.PasswordResetTokenRepository;
import com.internshipplatform.repository.UserRepository;
import com.internshipplatform.security.InputSanitizer;
import com.internshipplatform.security.JwtTokenProvider;
import com.internshipplatform.security.RateLimiter;
import com.internshipplatform.security.SecurityEventLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 15;
    private static final int VERIFICATION_TOKEN_EXPIRY_HOURS = 24;
    private static final String PURPOSE_EMAIL_VERIFICATION = "EMAIL_VERIFICATION";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SecurityEventLogger securityEventLogger;
    private final RateLimiter rateLimiter;
    private final EmailService emailService;
    private final HttpServletRequest request;

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

        // Sanitize inputs before storing
        User user = User.builder()
                .fullName(InputSanitizer.sanitize(request.getFullName()))
                .email(InputSanitizer.trimOnly(request.getEmail()).toLowerCase())
                .phoneNumber(InputSanitizer.trimOnly(request.getPhoneNumber()))
                .college(InputSanitizer.sanitize(request.getCollege()))
                .department(InputSanitizer.sanitize(request.getDepartment()))
                .graduationYear(InputSanitizer.trimOnly(request.getGraduationYear()))
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .emailVerified(false)
                .role("ROLE_USER")
                .build();

        user = userRepository.save(user);

        // Generate and send verification email
        String verificationToken = UUID.randomUUID().toString();
        EmailVerificationToken emailToken = EmailVerificationToken.builder()
                .token(verificationToken)
                .email(user.getEmail())
                .purpose(PURPOSE_EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(emailToken);

        String ipAddress = getClientIp();
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        securityEventLogger.logRegistration(user.getEmail(), ipAddress);
        securityEventLogger.logVerificationEmailSent(user.getEmail(), ipAddress);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        UserResponse userResponse = mapToUserResponse(user);

        return AuthResponse.of(accessToken, refreshToken, tokenProvider.getJwtExpiration(), userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String ipAddress = getClientIp();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            // Reset failed login attempts on successful login
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);

            // Generate tokens with user's current token version
            String accessToken = tokenProvider.generateAccessTokenFromEmail(email, user.getTokenVersion());
            String refreshToken = tokenProvider.generateRefreshTokenFromEmail(email, user.getTokenVersion());

            securityEventLogger.logLoginSuccess(email, ipAddress);

            UserResponse userResponse = mapToUserResponse(user);

            return AuthResponse.of(accessToken, refreshToken, tokenProvider.getJwtExpiration(), userResponse);

        } catch (BadCredentialsException e) {
            // Increment failed login attempts and potentially lock account
            userRepository.findByEmail(email).ifPresent(user -> {
                int newAttempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(newAttempts);

                if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                    user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
                    securityEventLogger.logAccountLocked(email, ipAddress, newAttempts);
                }

                userRepository.save(user);
            });

            securityEventLogger.logLoginFailure(email, ipAddress, "Bad credentials");
            throw e;

        } catch (DisabledException e) {
            securityEventLogger.logLoginFailure(email, ipAddress, "Account disabled");
            throw e;
        } catch (LockedException e) {
            securityEventLogger.logLoginFailure(email, ipAddress, "Account locked");
            throw e;
        }
    }

    public AuthResponse refreshTokens(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String ipAddress = getClientIp();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Token is not a refresh token");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        int tokenVersion = tokenProvider.getTokenVersionFromToken(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Validate token version
        if (user.getTokenVersion() != tokenVersion) {
            throw new BadCredentialsException("Token has been invalidated");
        }

        // Generate new tokens with current token version
        String newAccessToken = tokenProvider.generateAccessTokenFromEmail(email, user.getTokenVersion());
        String newRefreshToken = tokenProvider.generateRefreshTokenFromEmail(email, user.getTokenVersion());

        securityEventLogger.logTokenRefresh(email, ipAddress);

        UserResponse userResponse = mapToUserResponse(user);

        return AuthResponse.of(newAccessToken, newRefreshToken, tokenProvider.getJwtExpiration(), userResponse);
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToUserResponse(user);
    }

    /**
     * Change password for authenticated user.
     * Requires current password verification and increments token version to invalidate all existing tokens.
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request, String email) {
        String ipAddress = getClientIp();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Check if new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Update password and increment token version to invalidate all existing tokens
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        // Delete all existing reset tokens for this user
        passwordResetTokenRepository.deleteAllByEmail(email);

        securityEventLogger.logPasswordChange(email, ipAddress);
    }

    /**
     * Request a password reset token.
     * Always returns success message to prevent user enumeration.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String ipAddress = getClientIp();

        // Check rate limit
        if (!rateLimiter.isAllowed(email, ipAddress)) {
            securityEventLogger.logPasswordResetRateLimited(email, ipAddress);
            return;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        boolean userExists = userOpt.isPresent();

        securityEventLogger.logPasswordResetRequest(email, ipAddress, userExists);

        if (!userExists) {
            return;
        }

        // Delete any existing reset tokens for this email
        passwordResetTokenRepository.deleteAllByEmail(email);

        // Generate new reset token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(email)
                .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES))
                .used(false)
                .attemptCount(0)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send password reset email
        emailService.sendPasswordResetEmail(email, token);
    }

    /**
     * Reset password using a valid reset token.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String ipAddress = getClientIp();

        // Find the token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        // Validate token
        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        // Check attempt count to prevent brute force
        if (resetToken.getAttemptCount() >= 5) {
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);
            throw new IllegalArgumentException("Reset token has been locked due to too many failed attempts");
        }

        // Increment attempt count
        resetToken.setAttemptCount(resetToken.getAttemptCount() + 1);

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            passwordResetTokenRepository.save(resetToken);
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Find user
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password and increment token version
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Delete all other reset tokens for this user
        passwordResetTokenRepository.deleteAllByEmail(resetToken.getEmail());

        securityEventLogger.logPasswordReset(resetToken.getEmail(), ipAddress);
    }

    /**
     * Verify email using verification token.
     */
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenAndPurpose(
                token, PURPOSE_EMAIL_VERIFICATION)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (!verificationToken.isValid()) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        User user = userRepository.findByEmail(verificationToken.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        securityEventLogger.logEmailVerified(user.getEmail(), "system");
    }

    /**
     * Resend verification email for authenticated user.
     */
    @Transactional
    public void resendVerification(String email) {
        String ipAddress = getClientIp();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }

        // Delete existing verification tokens
        emailVerificationTokenRepository.deleteByEmailAndPurpose(email, PURPOSE_EMAIL_VERIFICATION);

        // Generate new verification token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .email(email)
                .purpose(PURPOSE_EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(email, token);
        securityEventLogger.logVerificationEmailSent(email, ipAddress);
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

    private String getClientIp() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
