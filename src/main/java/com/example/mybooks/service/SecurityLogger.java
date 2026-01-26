package com.example.mybooks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Task 3: Secure Logging Service
 *
 * This service handles all security-related logging with proper data sanitization.
 *
 * IMPORTANT RULES:
 * - NEVER log passwords
 * - NEVER log JWT tokens or refresh tokens
 * - NEVER log full PII (Personally Identifiable Information)
 * - ALWAYS sanitize user input before logging
 * - Log security events: failed logins, unauthorized access, suspicious activity
 */
@Service
public class SecurityLogger {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLogger.class);
    private static final Logger suspiciousLogger = LoggerFactory.getLogger("SUSPICIOUS_ACTIVITY");

    /**
     * Task 3.1: Log failed login attempts (without sensitive data)
     */
    public void logFailedLogin(String email, String ipAddress) {
        // Sanitize email to prevent log injection
        String sanitizedEmail = sanitizeInput(email);
        logger.warn("FAILED_LOGIN: email={}, ip={}", sanitizedEmail, ipAddress);
    }

    /**
     * Task 3.1: Log successful login
     */
    public void logSuccessfulLogin(String email, String ipAddress) {
        String sanitizedEmail = sanitizeInput(email);
        logger.info("SUCCESSFUL_LOGIN: email={}, ip={}", sanitizedEmail, ipAddress);
    }

    /**
     * Task 3.1: Log logout event
     */
    public void logLogout(String email, String ipAddress) {
        String sanitizedEmail = sanitizeInput(email);
        logger.info("LOGOUT: email={}, ip={}", sanitizedEmail, ipAddress);
    }

    /**
     * Task 3.1: Log unauthorized access attempts (401 errors)
     */
    public void logUnauthorizedAccess(String endpoint, String ipAddress, String reason) {
        logger.warn("UNAUTHORIZED_ACCESS: endpoint={}, ip={}, reason={}",
                sanitizeInput(endpoint), ipAddress, sanitizeInput(reason));
    }

    /**
     * Task 3.1: Log forbidden access attempts (403 errors)
     */
    public void logForbiddenAccess(String endpoint, String email, String ipAddress) {
        logger.warn("FORBIDDEN_ACCESS: endpoint={}, email={}, ip={}",
                sanitizeInput(endpoint), sanitizeInput(email), ipAddress);
    }

    /**
     * Task 3.2: Log suspicious request patterns
     */
    public void logSuspiciousActivity(String activityType, String details, String ipAddress) {
        suspiciousLogger.warn("SUSPICIOUS_ACTIVITY: type={}, details={}, ip={}",
                activityType, sanitizeInput(details), ipAddress);
    }

    /**
     * Log repeated invalid input (possible attack)
     */
    public void logRepeatedInvalidInput(String email, String inputType, int attemptCount) {
        logger.warn("REPEATED_INVALID_INPUT: email={}, type={}, attempts={}",
                sanitizeInput(email), inputType, attemptCount);
    }

    /**
     * Log SQL injection attempt
     */
    public void logSqlInjectionAttempt(String input, String ipAddress) {
        suspiciousLogger.warn("SQL_INJECTION_ATTEMPT: input={}, ip={}",
                sanitizeInput(input), ipAddress);
    }

    /**
     * Log XSS attempt
     */
    public void logXssAttempt(String input, String ipAddress) {
        suspiciousLogger.warn("XSS_ATTEMPT: input={}, ip={}",
                sanitizeInput(input), ipAddress);
    }

    /**
     * Log rate limiting trigger
     */
    public void logRateLimitExceeded(String ipAddress, String endpoint) {
        logger.warn("RATE_LIMIT_EXCEEDED: ip={}, endpoint={}", ipAddress, sanitizeInput(endpoint));
    }

    /**
     * Log token refresh
     */
    public void logTokenRefresh(String email, String ipAddress) {
        logger.info("TOKEN_REFRESH: email={}, ip={}", sanitizeInput(email), ipAddress);
    }

    /**
     * Log expired token usage attempt
     */
    public void logExpiredTokenUsage(String ipAddress) {
        logger.warn("EXPIRED_TOKEN_USAGE: ip={}", ipAddress);
    }

    /**
     * Log invalid token usage attempt
     */
    public void logInvalidTokenUsage(String ipAddress) {
        logger.warn("INVALID_TOKEN_USAGE: ip={}", ipAddress);
    }

    /**
     * Sanitize input to prevent log injection attacks
     *
     * Task 3.2: Prevent attackers from injecting malicious data into logs
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return "null";
        }

        // Remove newlines and carriage returns (prevent log forging)
        String sanitized = input.replace("\n", "")
                .replace("\r", "")
                .replace("\t", "");

        // Limit length to prevent log flooding
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200) + "...[truncated]";
        }

        return sanitized;
    }

    /**
     * Mask sensitive data (email example)
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "***";
        }

        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return "**@" + domain;
        }

        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    /**
     * NEVER log these:
     * - Passwords (plain or hashed)
     * - JWT tokens
     * - Refresh tokens
     * - Credit card numbers
     * - Social security numbers
     * - Full addresses
     */
    public void exampleOfWhatNOTToLog() {
        // ❌ NEVER DO THIS:
        // logger.info("User password: {}", password);
        // logger.info("JWT token: {}", jwtToken);
        // logger.info("User SSN: {}", ssn);

        // ✅ DO THIS INSTEAD:
        // logger.info("Password updated for user: {}", maskEmail(email));
        // logger.info("Token generated for user: {}", maskEmail(email));
    }
}