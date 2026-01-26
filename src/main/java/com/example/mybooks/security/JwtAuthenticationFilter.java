package com.example.mybooks.security;

import com.example.mybooks.service.SecurityLogger;
import com.example.mybooks.util.IpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SecurityLogger securityLogger;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService,
                                   SecurityLogger securityLogger) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.securityLogger = securityLogger;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String jwt = null;
        String username = null;
        String ipAddress = IpUtil.getClientIpAddress(request);

        try {
            // Try to get JWT from Authorization header
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }

            // If not in header, try to get from cookie
            if (jwt == null) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("jwt".equals(cookie.getName())) {
                            jwt = cookie.getValue();
                            break;
                        }
                    }
                }
            }

            // If JWT exists, validate it
            if (jwt != null) {
                try {
                    username = jwtService.extractUsername(jwt);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        if (jwtService.isTokenValid(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        } else {
                            // Task 3: Log invalid token usage
                            securityLogger.logInvalidTokenUsage(ipAddress);
                        }
                    }
                } catch (io.jsonwebtoken.ExpiredJwtException e) {
                    // Task 3: Log expired token usage attempt
                    securityLogger.logExpiredTokenUsage(ipAddress);
                } catch (io.jsonwebtoken.JwtException e) {
                    // Task 3: Log invalid token usage (malformed, signature invalid, etc.)
                    securityLogger.logInvalidTokenUsage(ipAddress);
                } catch (Exception e) {
                    // Task 3: Log suspicious JWT processing error
                    securityLogger.logSuspiciousActivity("JWT_PROCESSING_ERROR",
                            e.getMessage(), ipAddress);
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Task 3: Log unexpected filter error
            securityLogger.logSuspiciousActivity("FILTER_ERROR",
                    "Request: " + request.getRequestURI(), ipAddress);
            filterChain.doFilter(request, response);
        }
    }
}