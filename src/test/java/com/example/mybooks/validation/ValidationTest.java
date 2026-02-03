package com.example.mybooks.validation;

import com.example.mybooks.dto.CreateUserRequest;
import com.example.mybooks.dto.CreateBookRequest;
import com.example.mybooks.model.ReadingStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lab 14 - Task 1.3: Test Validation Rules
 *
 * Test input validation for DTOs
 * Ensure validation constraints work correctly
 */
@DisplayName("Validation Tests")
class ValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ========== CreateUserRequest Validation Tests ==========

    @Test
    @DisplayName("Should accept valid user request")
    void shouldAcceptValidUserRequest() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("valid@email.com");
        request.setPassword("ValidPass123!");

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    @DisplayName("Should reject username that is too short")
    void shouldRejectUsernameTooShort() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("ab");  // Too short (min 3)
        request.setEmail("valid@email.com");
        request.setPassword("ValidPass123!");

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("Should reject username that is too long")
    void shouldRejectUsernameTooLong() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("a".repeat(51));  // Too long (max 50)
        request.setEmail("valid@email.com");
        request.setPassword("ValidPass123!");

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("Should reject blank username")
    void shouldRejectBlankUsername() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("   ");  // Blank
        request.setEmail("valid@email.com");
        request.setPassword("ValidPass123!");

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Should reject null username")
    void shouldRejectNullUsername() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(null);
        request.setEmail("valid@email.com");
        request.setPassword("ValidPass123!");

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Should reject invalid email format")
    void shouldRejectInvalidEmailFormat() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("not-an-email");  // Invalid format
        request.setPassword("ValidPass123!");

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Should reject null email")
    void shouldRejectNullEmail() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail(null);
        request.setPassword("ValidPass123!");

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Should reject password that is too short")
    void shouldRejectPasswordTooShort() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("valid@email.com");
        request.setPassword("Sh1!");  // 4 karakter - min 6'dan az!

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Should reject password without uppercase letter")
    void shouldRejectPasswordWithoutUppercase() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("valid@email.com");
        request.setPassword("lowercase123!");  // No uppercase

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Should reject password without lowercase letter")
    void shouldRejectPasswordWithoutLowercase() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("valid@email.com");
        request.setPassword("UPPERCASE123!");  // No lowercase

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Should reject password without digit")
    void shouldRejectPasswordWithoutDigit() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("valid@email.com");
        request.setPassword("NoDigits!");  // No digit

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Should reject null password")
    void shouldRejectNullPassword() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("validuser");
        request.setEmail("valid@email.com");
        request.setPassword(null);

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Should report multiple validation errors")
    void shouldReportMultipleValidationErrors() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("ab");  // Too short
        request.setEmail("invalid");  // Invalid format
        request.setPassword("short");  // Too short + missing uppercase + missing digit

        // Act
        Set<ConstraintViolation<CreateUserRequest>> violations = validator.validate(request);

        // Assert
        assertTrue(violations.size() >= 3,
                "Should have at least 3 violations (username, email, password)");
    }

    // ========== CreateBookRequest Validation Tests ==========

    @Test
    @DisplayName("Should accept valid book request")
    void shouldAcceptValidBookRequest() {
        // Arrange
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("Valid Book Title");
        request.setAuthor("Valid Author");
        request.setIsbn("1234567890");
        request.setPublicationYear(2024);
        request.setGenre("Fiction");
        request.setReadingStatus(ReadingStatus.READING);
        request.setRating(4);

        // Act
        Set<ConstraintViolation<CreateBookRequest>> violations = validator.validate(request);

        // Assert
        assertTrue(violations.isEmpty(), "Valid book request should have no violations");
    }

    @Test
    @DisplayName("Should reject blank book title")
    void shouldRejectBlankBookTitle() {
        // Arrange
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("   ");  // Blank
        request.setAuthor("Valid Author");

        // Act
        Set<ConstraintViolation<CreateBookRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("Should reject null book title")
    void shouldRejectNullBookTitle() {
        // Arrange
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle(null);
        request.setAuthor("Valid Author");

        // Act
        Set<ConstraintViolation<CreateBookRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Should reject blank book author")
    void shouldRejectBlankBookAuthor() {
        // Arrange
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("Valid Title");
        request.setAuthor("   ");  // Blank

        // Act
        Set<ConstraintViolation<CreateBookRequest>> violations = validator.validate(request);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("author")));
    }

    @Test
    @DisplayName("Should accept book without optional fields")
    void shouldAcceptBookWithoutOptionalFields() {
        // Arrange
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("Valid Title");
        request.setAuthor("Valid Author");
        // isbn, publicationYear, genre, readingStatus, rating, notes are optional

        // Act
        Set<ConstraintViolation<CreateBookRequest>> violations = validator.validate(request);

        // Assert
        assertTrue(violations.isEmpty(),
                "Book with only required fields should be valid");
    }
}