package com.example.mybooks.service;

import com.example.mybooks.dto.CreateUserRequest;
import com.example.mybooks.model.Role;
import com.example.mybooks.model.User;
import com.example.mybooks.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Lab 14 - Task 1.1: Test Service Methods
 *
 * Unit tests for UserService - testing business logic in isolation
 * Mock repositories and external dependencies (no real database)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest validRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        // Setup valid user request for testing
        validRequest = new CreateUserRequest();
        validRequest.setUsername("testuser");
        validRequest.setEmail("test@test.com");
        validRequest.setPassword("Test123!@#");

        // Setup expected saved user
        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@test.com");
        savedUser.setPassword("encodedPassword123");
        savedUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("Should create user with encoded password")
    void shouldCreateUserWithEncodedPassword() {
        // Arrange -
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.createUser(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@test.com", result.getEmail());
        assertEquals("encodedPassword123", result.getPassword());

        // Verify interactions
        verify(passwordEncoder, times(1)).encode("Test123!@#");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert - IllegalArgumentException bekle
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(validRequest)
        );

        assertEquals("Username already exists", exception.getMessage());

        // Verify that save was never called
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(validRequest)
        );

        assertEquals("Email already exists", exception.getMessage());

        // Verify that save was never called
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        // Arrange
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(savedUser));

        // Act
        Optional<User> result = userService.findByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void shouldReturnEmptyWhenUserNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent"))
                .thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername("nonexistent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should authenticate user with correct credentials")
    void shouldAuthenticateUserWithCorrectCredentials() {
        // Arrange
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("Test123!@#", "encodedPassword123"))
                .thenReturn(true);

        // Act
        User result = userService.authenticate("test@test.com", "Test123!@#");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(passwordEncoder, times(1)).matches("Test123!@#", "encodedPassword123");
    }

    @Test
    @DisplayName("Should throw exception for wrong password")
    void shouldThrowExceptionForWrongPassword() {
        // Arrange
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("WrongPassword", "encodedPassword123"))
                .thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.authenticate("test@test.com", "WrongPassword")
        );

        assertEquals("Invalid credentials", exception.getMessage());
    }
}