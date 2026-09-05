package com.weathergpt.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for password reset requests.
 * Limits by email and by IP address.
 */
@Component
public class RateLimiter {

    private static final int MAX_REQUESTS_PER_EMAIL = 3;
    private static final int MAX_REQUESTS_PER_IP = 10;
    private static final long WINDOW_MS = 60 * 60 * 1000; // 1 hour

    private final ConcurrentHashMap<String, RateLimitEntry> emailLimits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateLimitEntry> ipLimits = new ConcurrentHashMap<>();

    /**
     * Check if a request from the given email and IP is allowed.
     * Returns true if allowed, false if rate limited.
     */
    public boolean isAllowed(String email, String ipAddress) {
        long now = System.currentTimeMillis();

        // Check email-based limit
        RateLimitEntry emailEntry = emailLimits.compute(email.toLowerCase(), (key, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateLimitEntry(now);
            }
            return existing;
        });

        if (emailEntry.count.incrementAndGet() > MAX_REQUESTS_PER_EMAIL) {
            return false;
        }

        // Check IP-based limit
        RateLimitEntry ipEntry = ipLimits.compute(ipAddress, (key, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new RateLimitEntry(now);
            }
            return existing;
        });

        return ipEntry.count.incrementAndGet() <= MAX_REQUESTS_PER_IP;
    }

    /**
     * Get remaining attempts for an email.
     */
    public int getRemainingAttempts(String email) {
        RateLimitEntry entry = emailLimits.get(email.toLowerCase());
        if (entry == null || System.currentTimeMillis() - entry.windowStart > WINDOW_MS) {
            return MAX_REQUESTS_PER_EMAIL;
        }
        return Math.max(0, MAX_REQUESTS_PER_EMAIL - entry.count.get());
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
        }
    }
}
