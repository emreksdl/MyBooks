package com.example.mybooks.controller;

import com.example.mybooks.service.RateLimiterService;
import com.example.mybooks.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin endpoint for rate limiter management
 * (Optional - useful for testing and monitoring)
 */
@RestController
@RequestMapping("/api/admin/rate-limit")
public class RateLimitAdminController {

    private final RateLimiterService rateLimiterService;

    public RateLimitAdminController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Get rate limit statistics
     * Only accessible by ADMIN role
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStatistics() {
        Map<String, Object> stats = rateLimiterService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Check rate limit status for current IP
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(HttpServletRequest request) {
        String ipAddress = IpUtil.getClientIpAddress(request);

        Map<String, Object> status = new HashMap<>();
        status.put("ipAddress", ipAddress);
        status.put("loginRemaining", rateLimiterService.getRemainingLoginAttempts(ipAddress));
        status.put("loginBlocked", rateLimiterService.isBlocked(ipAddress, "login"));

        if (rateLimiterService.isBlocked(ipAddress, "login")) {
            long retryAfter = rateLimiterService.getTimeUntilUnblock(ipAddress, "login", 60);
            status.put("retryAfterSeconds", retryAfter);
        }

        return ResponseEntity.ok(status);
    }

    /**
     * Reset rate limit for specific IP
     * Only accessible by ADMIN role
     */
    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetRateLimit(
            @RequestParam String ipAddress,
            @RequestParam(defaultValue = "login") String limitType) {

        rateLimiterService.reset(ipAddress, limitType);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Rate limit reset successfully");
        response.put("ipAddress", ipAddress);
        response.put("limitType", limitType);

        return ResponseEntity.ok(response);
    }
}