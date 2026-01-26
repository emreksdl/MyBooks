package com.example.mybooks.config;

import com.example.mybooks.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final RateLimiterFilter rateLimiterFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          SecurityHeadersFilter securityHeadersFilter,
                          RateLimiterFilter rateLimiterFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.securityHeadersFilter = securityHeadersFilter;
        this.rateLimiterFilter = rateLimiterFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Task 5: HTTPS Configuration
                .requiresChannel(channel -> {
                    if (sslEnabled) {
                        // Require HTTPS for all requests
                        channel.anyRequest().requiresSecure();
                    }
                })

                // Task 2: Security Headers Configuration
                .headers(headers -> headers
                        // X-Content-Type-Options: nosniff
                        .contentTypeOptions(contentTypeOptions -> contentTypeOptions.disable()) // Handled by custom filter

                        // X-Frame-Options: DENY
                        .frameOptions(frameOptions -> frameOptions.deny())

                        // X-XSS-Protection
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )

                        // Referrer-Policy
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )

                        // Content-Security-Policy
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com; " +
                                        "style-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com")
                        )

                        // Task 5.3: HSTS (Strict-Transport-Security) - configured in SecurityHeadersFilter
                        .httpStrictTransportSecurity(hsts -> {
                            if (sslEnabled) {
                                hsts.includeSubDomains(true)
                                        .maxAgeInSeconds(31536000) // 1 year
                                        .preload(true);
                            }
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/register.html",
                                "/login.html",
                                "/dashboard.html",
                                "/books.html",
                                "/notes.html",
                                "/*.css",
                                "/*.js",
                                "/hello",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/info",
                                "/api/health",
                                "/api/upload/book-covers/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Task 4: Add Rate Limiter Filter
                .addFilterBefore(rateLimiterFilter, UsernamePasswordAuthenticationFilter.class)
                // Task 2: Add Security Headers Filter
                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
                // JWT Authentication Filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}