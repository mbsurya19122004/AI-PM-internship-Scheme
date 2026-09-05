package com.weathergpt.security;

/**
 * Utility class to sanitize user inputs before storing them in the database.
 * Prevents stored XSS by stripping HTML tags and dangerous characters.
 */
public final class InputSanitizer {

    private InputSanitizer() {
        // utility class
    }

    /**
     * Sanitizes a string by removing HTML tags and trimming whitespace.
     * Returns null if the input is null.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // Remove HTML tags
        String sanitized = input.replaceAll("<[^>]*>", "");
        // Encode common HTML special characters that might slip through
        sanitized = sanitized
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
        return sanitized.trim();
    }

    /**
     * Sanitizes a string by only trimming whitespace (for fields where
     * HTML encoding is not desired, like names or emails).
     * Returns null if the input is null.
     */
    public static String trimOnly(String input) {
        return input != null ? input.trim() : null;
    }
}
