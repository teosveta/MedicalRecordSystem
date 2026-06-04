package com.medicalrecord.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

// Проверява дали датата не е повече от 2 дни назад
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = NotTooOldValidator.class)
public @interface NotTooOld {

    String message() default "Началната дата не може да бъде повече от 2 дни назад";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
