package com.example.mybooks.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {

    String message() default "Username must contain only letters, numbers, and underscores, and cannot start with a number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}