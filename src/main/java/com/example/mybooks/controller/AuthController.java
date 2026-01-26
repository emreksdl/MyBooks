package com.example.mybooks.controller;

import com.example.mybooks.model.RefreshToken;
import com.example.mybooks.model.User;
import com.example.mybooks.dto.*;
import com.example.mybooks.security.JwtService;
import com.example.mybooks.service.RefreshTokenService;
import com.example.mybooks.service.SecurityLogger;
import com.example.mybooks.service.UserService;
import com.example.mybooks.util.CookieUtil;
import com.example.mybooks.util.IpUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final SecurityLogger securityLogger;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          UserDetailsService userDetailsService,
                          SecurityLogger securityLogger) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.securityLogger = securityLogger;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody CreateUserRequest request,
                                      HttpServletRequest httpRequest) {
        try {
            User user = userService.createUser(request);

            // Task 3: Log registration (without password!)
            String ipAddress = IpUtil.getClientIpAddress(httpRequest);
            securityLogger.logSuccessfulLogin(user.getEmail(), ipAddress);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            // Task 3: Log failed registration
            String ipAddress = IpUtil.getClientIpAddress(httpRequest);
            securityLogger.logSuspiciousActivity("REGISTRATION_FAILED",
                    "Email: " + request.getEmail(), ipAddress);
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            @RequestParam(defaultValue = "jwt") String authType,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String ipAddress = IpUtil.getClientIpAddress(httpRequest);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // JWT-based authentication
            if ("jwt".equalsIgnoreCase(authType)) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String jwtToken = jwtService.generateToken(userDetails);

                // Create refresh token
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

                // Task 2.5: Use secure cookies
                httpResponse.addHeader("Set-Cookie",
                        String.format("%s=%s; Path=/; HttpOnly; %s SameSite=Strict; Max-Age=%d",
                                "jwt", jwtToken, sslEnabled ? "Secure;" : "", 15 * 60));

                httpResponse.addHeader("Set-Cookie",
                        String.format("%s=%s; Path=/; HttpOnly; %s SameSite=Strict; Max-Age=%d",
                                "refreshToken", refreshToken.getToken(), sslEnabled ? "Secure;" : "", 7 * 24 * 60 * 60));

                // Task 3.1: Log successful login (WITHOUT TOKEN!)
                securityLogger.logSuccessfulLogin(user.getEmail(), ipAddress);

                JwtAuthResponse response = new JwtAuthResponse(
                        jwtToken,
                        refreshToken.getToken(),
                        user.getId(),
                        user.getUsername(),
                        user.getEmail()
                );

                return ResponseEntity.ok(response);
            }
            // Session-based authentication
            else {
                SecurityContext securityContext = SecurityContextHolder.getContext();
                securityContext.setAuthentication(authentication);

                HttpSession session = httpRequest.getSession(true);
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

                securityLogger.logSuccessfulLogin(user.getEmail(), ipAddress);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Login successful");
                response.put("userId", user.getId());
                response.put("username", user.getUsername());
                response.put("sessionId", session.getId());

                return ResponseEntity.ok(response);
            }
        } catch (BadCredentialsException e) {
            // Task 3.1: Log failed login attempt (WITHOUT PASSWORD!)
            securityLogger.logFailedLogin(request.getEmail(), ipAddress);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                          HttpServletRequest httpRequest) {
        String refreshTokenString = request.getRefreshToken();
        String ipAddress = IpUtil.getClientIpAddress(httpRequest);

        try {
            RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenString)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            // Verify expiration
            refreshToken = refreshTokenService.verifyExpiration(refreshToken);

            User user = refreshToken.getUser();

            // Generate new access token
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String newAccessToken = jwtService.generateToken(userDetails);

            // ROTATION: Create new refresh token and revoke old one
            refreshTokenService.revokeToken(refreshTokenString);
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

            // Task 3: Log token refresh (WITHOUT TOKEN!)
            securityLogger.logTokenRefresh(user.getEmail(), ipAddress);

            RefreshTokenResponse response = new RefreshTokenResponse(
                    newAccessToken,
                    newRefreshToken.getToken()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Task 3: Log suspicious token refresh attempt
            securityLogger.logSuspiciousActivity("TOKEN_REFRESH_FAILED",
                    "Reason: " + e.getMessage(), ipAddress);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Authentication authentication) {
        String ipAddress = IpUtil.getClientIpAddress(request);

        // Clear cookies
        Cookie jwtCookie = CookieUtil.deleteCookie("jwt");
        Cookie refreshCookie = CookieUtil.deleteCookie("refreshToken");

        response.addCookie(jwtCookie);
        response.addCookie(refreshCookie);

        // Revoke refresh token from database
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            userService.findByEmail(email).ifPresent(user -> {
                refreshTokenService.deleteByUserId(user.getId());

                // Task 3: Log logout
                securityLogger.logLogout(email, ipAddress);
            });
        }

        // Clear session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication,
                                            HttpServletRequest request) {
        String ipAddress = IpUtil.getClientIpAddress(request);

        if (authentication == null || !authentication.isAuthenticated()) {
            // Task 3.1: Log unauthorized access
            securityLogger.logUnauthorizedAccess("/api/auth/me", ipAddress, "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());

        return ResponseEntity.ok(response);
    }
}