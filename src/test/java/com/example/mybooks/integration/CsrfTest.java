package com.example.mybooks.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Lab 14 - Task 2.2: CSRF Protection Tests
 *
 * Our application uses stateless JWT authentication (REST API).
 * CSRF protection is disabled because:
 * 1. We don't use cookies for authentication (JWT in HTTP-only cookies but with proper SameSite)
 * 2. Stateless session management
 * 3. No state-changing GET requests
 *
 * This test verifies CSRF is properly disabled for our REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CSRF Protection Tests")
class CsrfTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should allow POST requests without CSRF token (CSRF disabled for REST API)")
    void shouldAllowPostWithoutCsrfToken() throws Exception {
        // For REST API with JWT, CSRF protection should be disabled
        // This request should NOT fail with 403 Forbidden due to missing CSRF token

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"Test123!\"}"))
                // Should not return 403 (which would indicate CSRF protection is active)
                .andExpect(status().isCreated()); // 201 - successfully created without CSRF token
    }

    @Test
    @DisplayName("Should allow unauthenticated POST to public endpoints")
    void shouldAllowPublicPostEndpoints() throws Exception {
        // Login endpoint should work without CSRF token
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nonexistent@example.com\",\"password\":\"wrong\"}"))
                // Should return 401 (wrong credentials) not 403 (CSRF)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Verify CSRF is disabled in SecurityConfig")
    void verifyCsrfIsDisabled() throws Exception {
        // If CSRF were enabled, ANY POST without CSRF token would fail with 403
        // Since it's disabled for our REST API, we should get proper application response

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"csrfuser\",\"email\":\"csrfuser@test.com\",\"password\":\"Test123!\"}"))
                // Should succeed with 201, proving CSRF is disabled
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("csrfuser@test.com"));
    }
}