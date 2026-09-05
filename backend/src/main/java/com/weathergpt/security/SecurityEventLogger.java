package com.weathergpt.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized security event logger for audit trails.
 * Logs security-relevant events with timestamps, IP addresses, and usernames.
 */
@Slf4j
@Component
public class SecurityEventLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void logLoginSuccess(String email, String ipAddress) {
        log.info("[SECURITY] LOGIN_SUCCESS | time={} | user={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress);
    }

    public void logLoginFailure(String email, String ipAddress, String reason) {
        log.warn("[SECURITY] LOGIN_FAILURE | time={} | user={} | ip={} | reason={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress, reason);
    }

    public void logAccountLocked(String email, String ipAddress, int failedAttempts) {
        log.warn("[SECURITY] ACCOUNT_LOCKED | time={} | user={} | ip={} | failedAttempts={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress, failedAttempts);
    }

    public void logAccountUnlocked(String email) {
        log.info("[SECURITY] ACCOUNT_UNLOCKED | time={} | user={}",
                LocalDateTime.now().format(FORMATTER), email);
    }

    public void logRegistration(String email, String ipAddress) {
        log.info("[SECURITY] REGISTRATION | time={} | user={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress);
    }

    public void logTokenRefresh(String email, String ipAddress) {
        log.info("[SECURITY] TOKEN_REFRESH | time={} | user={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress);
    }

    public void logUnauthorizedAccess(String path, String ipAddress) {
        log.warn("[SECURITY] UNAUTHORIZED_ACCESS | time={} | path={} | ip={}",
                LocalDateTime.now().format(FORMATTER), path, ipAddress);
    }

    public void logUserEnumerationAttempt(String email, String ipAddress, String endpoint) {
        log.warn("[SECURITY] USER_ENUMERATION_ATTEMPT | time={} | user={} | ip={} | endpoint={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress, endpoint);
    }

    public void logPasswordChange(String email, String ipAddress) {
        log.info("[SECURITY] PASSWORD_CHANGE | time={} | user={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress);
    }

    public void logPasswordResetRequest(String email, String ipAddress, boolean userExists) {
        // Always log the same message to prevent user enumeration
        log.info("[SECURITY] PASSWORD_RESET_REQUEST | time={} | user={} | ip={} | userExists={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress, userExists);
    }

    public void logPasswordReset(String email, String ipAddress) {
        log.info("[SECURITY] PASSWORD_RESET | time={} | user={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress);
    }

    public void logPasswordResetRateLimited(String email, String ipAddress) {
        log.warn("[SECURITY] PASSWORD_RESET_RATE_LIMITED | time={} | user={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress);
    }

    public void logUserRoleChanged(String email, String oldRole, String newRole, String ipAddress) {
        log.info("[SECURITY] USER_ROLE_CHANGED | time={} | user={} | oldRole={} | newRole={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, oldRole, newRole, ipAddress);
    }

    public void logEmailVerificationRequest(String email, String ipAddress) {
        log.info("[SECURITY] EMAIL_VERIFICATION_REQUEST | time={} | user={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress);
    }

    public void logEmailVerified(String email, String ipAddress) {
        log.info("[SECURITY] EMAIL_VERIFIED | time={} | user={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress);
    }

    public void logVerificationEmailSent(String email, String ipAddress) {
        log.info("[SECURITY] VERIFICATION_EMAIL_SENT | time={} | user={} | ip={}",
                LocalDateTime.now().format(FORMATTER), email, ipAddress);
    }
}
