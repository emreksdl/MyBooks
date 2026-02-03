package com.example.mybooks.config;

import com.example.mybooks.service.RateLimiterService;
import com.example.mybooks.service.SecurityLogger;
import com.example.mybooks.util.IpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;  // ← YENİ
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Task 4: Rate Limiting Filter
 *
 * Applies rate limiting to sensitive endpoints to prevent brute-force attacks.
 */
@Component
@Order(1)
@ConditionalOnProperty(
        name = "rate.limit.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RateLimiterFilter extends OncePerRequestFilter {


    private final RateLimiterService rateLimiterService;
    private final SecurityLogger securityLogger;
    private final ObjectMapper objectMapper;

    public RateLimiterFilter(RateLimiterService rateLimiterService,
                             SecurityLogger securityLogger) {
        this.rateLimiterService = rateLimiterService;
        this.securityLogger = securityLogger;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String ipAddress = IpUtil.getClientIpAddress(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // Apply rate limiting to specific endpoints
        boolean allowed = true;
        String limitType = null;
        int remainingAttempts = 0;

        // Login endpoint - strict rate limiting
        if (requestUri.equals("/api/auth/login") && method.equals("POST")) {
            allowed = rateLimiterService.isLoginAllowed(ipAddress);
            limitType = "login";
            remainingAttempts = rateLimiterService.getRemainingLoginAttempts(ipAddress);
        }
        // Registration endpoint - moderate rate limiting
        else if (requestUri.equals("/api/auth/register") && method.equals("POST")) {
            allowed = rateLimiterService.isRegistrationAllowed(ipAddress);
            limitType = "register";
        }
        // Other API endpoints - generous rate limiting
        else if (requestUri.startsWith("/api/") && !requestUri.startsWith("/api/auth/")) {
            allowed = rateLimiterService.isApiAllowed(ipAddress);
            limitType = "api";
        }

        if (!allowed) {
            // Task 4: Log rate limit exceeded
            securityLogger.logRateLimitExceeded(ipAddress, requestUri);

            // Return 429 Too Many Requests
            response.setStatus(429);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Too many requests. Please try again later.");
            errorResponse.put("error", "RATE_LIMIT_EXCEEDED");

            if (limitType != null) {
                long retryAfter = rateLimiterService.getTimeUntilUnblock(ipAddress, limitType, 60);
                errorResponse.put("retryAfter", retryAfter);
                response.setHeader("Retry-After", String.valueOf(retryAfter));
            }

            if (limitType != null && limitType.equals("login")) {
                errorResponse.put("remainingAttempts", remainingAttempts);
            }

            errorResponse.put("timestamp", System.currentTimeMillis());

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        // Add rate limit headers to response (informational)
        if (limitType != null && limitType.equals("login")) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remainingAttempts));
            response.setHeader("X-RateLimit-Limit", "5");
        }

        filterChain.doFilter(request, response);
    }
}