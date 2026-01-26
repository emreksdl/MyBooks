package com.example.mybooks.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Task 2.1: X-Content-Type-Options
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Task 2.2: X-Frame-Options
        response.setHeader("X-Frame-Options", "DENY");

        // Task 2.3: Content-Security-Policy
        String csp = "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com; " +
                "style-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com; " +
                "img-src 'self' data:; " +
                "font-src 'self' data:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none';";
        response.setHeader("Content-Security-Policy", csp);

        // Task 2.4: Referrer-Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Task 5.3: HSTS (Strict-Transport-Security) - Only if HTTPS is enabled
        if (sslEnabled && request.isSecure()) {
            // max-age=31536000 (1 year)
            // includeSubDomains - apply to all subdomains
            // preload - eligible for browser preload list
            response.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains; preload");
        }

        // X-XSS-Protection (legacy, but still useful for older browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Permissions-Policy
        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=()");

        // Cache-Control for sensitive pages
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        filterChain.doFilter(request, response);
    }
}