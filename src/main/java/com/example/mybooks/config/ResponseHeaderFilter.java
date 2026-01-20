package com.example.mybooks.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ResponseHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Set standard headers for all responses
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Set API version header
        response.setHeader("X-API-Version", "1.0.0");

        // Ensure JSON responses have correct Content-Type
        if (request.getRequestURI().startsWith("/api/") &&
                !request.getRequestURI().contains("/upload/book-covers/")) {
            response.setHeader("Content-Type", "application/json; charset=UTF-8");
        }

        filterChain.doFilter(request, response);
    }
}