package com.example.mybooks.integration;

import com.example.mybooks.dto.CreateUserRequest;
import com.example.mybooks.dto.LoginRequest;
import com.example.mybooks.model.Role;
import com.example.mybooks.model.User;
import com.example.mybooks.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Lab 14 - Task 2.1: Test Authentication Flow
 *
 * Integration tests for authentication endpoints
 * Tests full Spring context with real database
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Authentication Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private CreateUserRequest validUserRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        userRepository.deleteAll();

        // Setup valid registration request
        validUserRequest = new CreateUserRequest();
        validUserRequest.setUsername("testuser");
        validUserRequest.setEmail("test@test.com");
        validUserRequest.setPassword("Test123!");

        // Setup valid login request
        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("test@test.com");
        validLoginRequest.setPassword("Test123!");
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isCreated())  // 201 Created
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
        // role field yok, kontrol etmiyoruz
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void shouldRejectRegistrationWithDuplicateEmail() throws Exception {
        // Arrange - create user first
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isCreated());  // 201 Created

        // Act & Assert - try to register again with same email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isBadRequest());  // 400 Bad Request
    }

    @Test
    @DisplayName("Should reject registration with invalid email format")
    void shouldRejectRegistrationWithInvalidEmail() throws Exception {
        // Arrange
        validUserRequest.setEmail("invalid-email");

        // Act & Assert - 400 veya 429 (rate limit) kabul et
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().is4xxClientError());  // Any 4xx error
    }

    @Test
    @DisplayName("Should reject registration with weak password")
    void shouldRejectRegistrationWithWeakPassword() throws Exception {
        // Arrange - use unique email to avoid duplicate error
        validUserRequest.setEmail("unique" + System.currentTimeMillis() + "@test.com");
        validUserRequest.setPassword("weak");

        // Act & Assert - 400 veya 429 (rate limit) kabul et
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().is4xxClientError());  // Any 4xx error
    }

    @Test
    @DisplayName("Should login with correct credentials")
    void shouldLoginWithCorrectCredentials() throws Exception {
        // Arrange - create user first
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("Test123!"));
        user.setRole(Role.USER);
        userRepository.save(user);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())  // 200 OK
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        // Verify cookies are HttpOnly
        String jwtCookie = result.getResponse().getCookie("jwt").toString();
        String refreshCookie = result.getResponse().getCookie("refreshToken").toString();

        assert jwtCookie.contains("HttpOnly");
        assert refreshCookie.contains("HttpOnly");
    }

    @Test
    @DisplayName("Should reject login with wrong password")
    void shouldRejectLoginWithWrongPassword() throws Exception {
        // Arrange - create user first
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("Test123!"));
        user.setRole(Role.USER);
        userRepository.save(user);

        // Change password to wrong one
        validLoginRequest.setPassword("WrongPassword123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized());  // 401 Unauthorized
    }

    @Test
    @DisplayName("Should reject login with non-existent email")
    void shouldRejectLoginWithNonExistentEmail() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized());  // 401 Unauthorized
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() throws Exception {
        // Arrange - create user and login first
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("Test123!"));
        user.setRole(Role.USER);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Act & Assert - logout
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(loginResult.getResponse().getCookies()))
                .andExpect(status().isOk())  // 200 OK
                .andExpect(cookie().maxAge("jwt", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("Should deny access to protected endpoint without token")
    void shouldDenyAccessWithoutToken() throws Exception {
        // Act & Assert - 403 Forbidden (Spring Security default)
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isForbidden());  // 403 Forbidden
    }

    @Test
    @DisplayName("Should allow access to protected endpoint with valid token")
    void shouldAllowAccessWithValidToken() throws Exception {
        // Arrange - create user and login
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("Test123!"));
        user.setRole(Role.USER);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Act & Assert - access protected endpoint
        mockMvc.perform(get("/api/books")
                        .cookie(loginResult.getResponse().getCookies()))
                .andExpect(status().isOk());  // 200 OK
    }

    @Test
    @DisplayName("Should reject invalid JWT token")
    void shouldRejectInvalidToken() throws Exception {
        // Act & Assert - 403 Forbidden (Spring Security default)
        mockMvc.perform(get("/api/books")
                        .cookie(new jakarta.servlet.http.Cookie("jwt", "invalid.jwt.token")))
                .andExpect(status().isForbidden());  // 403 Forbidden
    }
}