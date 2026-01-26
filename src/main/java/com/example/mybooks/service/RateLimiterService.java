package com.example.mybooks.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task 4: Simple In-Memory Rate Limiter
 *
 * Prevents brute-force attacks by limiting requests per IP address.
 *
 * Features:
 * - Sliding window algorithm
 * - Configurable limits per endpoint type
 * - Automatic cleanup of old entries
 * - Thread-safe (ConcurrentHashMap)
 */
@Service
public class RateLimiterService {

    // Store: IP -> RequestInfo
    private final Map<String, RequestInfo> requestStore = new ConcurrentHashMap<>();

    // Cleanup interval (5 minutes)
    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000;
    private long lastCleanup = System.currentTimeMillis();

    /**
     * Check if request is allowed for login endpoint
     * Limit: 5 attempts per minute
     */
    public boolean isLoginAllowed(String ipAddress) {
        return isAllowed(ipAddress, "login", 5, 60);
    }

    /**
     * Check if request is allowed for API endpoints
     * Limit: 100 requests per minute
     */
    public boolean isApiAllowed(String ipAddress) {
        return isAllowed(ipAddress, "api", 100, 60);
    }

    /**
     * Check if request is allowed for registration
     * Limit: 3 attempts per minute
     */
    public boolean isRegistrationAllowed(String ipAddress) {
        return isAllowed(ipAddress, "register", 3, 60);
    }

    /**
     * Generic rate limiting logic
     *
     * @param ipAddress Client IP address
     * @param key Unique key for this rate limit (e.g., "login", "api")
     * @param maxRequests Maximum requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if allowed, false if rate limit exceeded
     */
    private boolean isAllowed(String ipAddress, String key, int maxRequests, int windowSeconds) {
        // Cleanup old entries periodically
        cleanupIfNeeded();

        String limitKey = ipAddress + ":" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        RequestInfo info = requestStore.computeIfAbsent(limitKey, k -> new RequestInfo());

        synchronized (info) {
            // Remove old timestamps outside the window
            info.timestamps.removeIf(timestamp -> timestamp < windowStart);

            // Check if limit exceeded
            if (info.timestamps.size() >= maxRequests) {
                info.blocked = true;
                info.lastBlockedTime = now;
                return false;
            }

            // Add current request timestamp
            info.timestamps.add(now);
            info.blocked = false;
            return true;
        }
    }

    /**
     * Get remaining attempts for login
     */
    public int getRemainingLoginAttempts(String ipAddress) {
        return getRemainingAttempts(ipAddress, "login", 5, 60);
    }

    /**
     * Get remaining attempts
     */
    private int getRemainingAttempts(String ipAddress, String key, int maxRequests, int windowSeconds) {
        String limitKey = ipAddress + ":" + key;
        RequestInfo info = requestStore.get(limitKey);

        if (info == null) {
            return maxRequests;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        synchronized (info) {
            // Count requests in current window
            long currentRequests = info.timestamps.stream()
                    .filter(timestamp -> timestamp >= windowStart)
                    .count();

            return Math.max(0, maxRequests - (int) currentRequests);
        }
    }

    /**
     * Check if IP is currently blocked
     */
    public boolean isBlocked(String ipAddress, String key) {
        String limitKey = ipAddress + ":" + key;
        RequestInfo info = requestStore.get(limitKey);

        if (info == null) {
            return false;
        }

        synchronized (info) {
            return info.blocked;
        }
    }

    /**
     * Get time until unblock (in seconds)
     */
    public long getTimeUntilUnblock(String ipAddress, String key, int windowSeconds) {
        String limitKey = ipAddress + ":" + key;
        RequestInfo info = requestStore.get(limitKey);

        if (info == null || !info.blocked) {
            return 0;
        }

        synchronized (info) {
            if (info.timestamps.isEmpty()) {
                return 0;
            }

            long oldestTimestamp = info.timestamps.get(0);
            long now = System.currentTimeMillis();
            long windowMillis = windowSeconds * 1000L;
            long unblockTime = oldestTimestamp + windowMillis;

            return Math.max(0, (unblockTime - now) / 1000);
        }
    }

    /**
     * Reset rate limit for specific IP and key (for testing or admin purposes)
     */
    public void reset(String ipAddress, String key) {
        String limitKey = ipAddress + ":" + key;
        requestStore.remove(limitKey);
    }

    /**
     * Cleanup old entries to prevent memory leak
     */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL) {
            cleanup();
            lastCleanup = now;
        }
    }

    /**
     * Remove entries older than 10 minutes
     */
    private void cleanup() {
        long threshold = System.currentTimeMillis() - (10 * 60 * 1000);

        requestStore.entrySet().removeIf(entry -> {
            RequestInfo info = entry.getValue();
            synchronized (info) {
                // Remove if all timestamps are old
                info.timestamps.removeIf(timestamp -> timestamp < threshold);
                return info.timestamps.isEmpty();
            }
        });
    }

    /**
     * Get statistics (for monitoring)
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "totalTrackedIPs", requestStore.size(),
                "lastCleanup", Instant.ofEpochMilli(lastCleanup).toString()
        );
    }

    /**
     * Internal class to store request information
     */
    private static class RequestInfo {
        final java.util.List<Long> timestamps = new java.util.concurrent.CopyOnWriteArrayList<>();
        boolean blocked = false;
        long lastBlockedTime = 0;
    }
}