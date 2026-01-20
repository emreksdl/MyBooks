package com.example.mybooks.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private static final String USERNAME_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]*$";

    @Override
    public void initialize(ValidUsername constraintAnnotation) {
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isEmpty()) {
            return true; // @NotBlank will handle null/empty check
        }

        // Check if username matches pattern
        if (!username.matches(USERNAME_PATTERN)) {
            return false;
        }

        // Additional rule: username cannot be reserved words
        String lowerUsername = username.toLowerCase();
        if (lowerUsername.equals("admin") ||
                lowerUsername.equals("root") ||
                lowerUsername.equals("system")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Username '" + username + "' is reserved and cannot be used"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}