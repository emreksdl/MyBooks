package com.example.mybooks.exception;

import com.example.mybooks.service.SecurityLogger;
import com.example.mybooks.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SecurityLogger securityLogger;

    public GlobalExceptionHandler(SecurityLogger securityLogger) {
        this.securityLogger = securityLogger;
    }

    /**
     * Task 3.1: Log and handle 401 Unauthorized errors
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        String ipAddress = IpUtil.getClientIpAddress(request);
        String endpoint = request.getRequestURI();

        // Task 3.1: Log unauthorized access attempt
        securityLogger.logUnauthorizedAccess(endpoint, ipAddress, ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Authentication failed");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Task 3.1: Log and handle 403 Forbidden errors
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        String ipAddress = IpUtil.getClientIpAddress(request);
        String endpoint = request.getRequestURI();

        // Get authenticated user if available
        String email = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : "anonymous";

        // Task 3.1: Log forbidden access attempt
        securityLogger.logForbiddenAccess(endpoint, email, ipAddress);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Access denied");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Task 3.2: Log and handle validation errors (repeated invalid input)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String ipAddress = IpUtil.getClientIpAddress(request);

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Task 3.2: Log suspicious repeated invalid input
        if (errors.size() > 3) {
            securityLogger.logSuspiciousActivity("MULTIPLE_VALIDATION_ERRORS",
                    "Fields: " + errors.keySet(), ipAddress);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", errors);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle bad credentials exception
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        String ipAddress = IpUtil.getClientIpAddress(request);

        // Already logged in AuthController, just return response
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Invalid credentials");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        String ipAddress = IpUtil.getClientIpAddress(request);

        // Check if it might be a suspicious request
        String message = ex.getMessage();
        if (message != null && (message.contains("DROP") || message.contains("DELETE") ||
                message.contains("<script>") || message.contains("javascript:"))) {
            securityLogger.logSuspiciousActivity("SUSPICIOUS_INPUT", message, ipAddress);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Invalid request");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle security exceptions (from service layer)
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(
            SecurityException ex, HttpServletRequest request) {

        String ipAddress = IpUtil.getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        String email = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : "anonymous";

        // Task 3.1: Log forbidden access (data-level)
        securityLogger.logForbiddenAccess(endpoint, email, ipAddress);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Access denied");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Generic exception handler (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String ipAddress = IpUtil.getClientIpAddress(request);

        // Log error (but not sensitive details)
        securityLogger.logSuspiciousActivity("SERVER_ERROR",
                "Endpoint: " + request.getRequestURI(), ipAddress);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Internal server error");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}