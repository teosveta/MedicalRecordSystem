package com.medicalrecord.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class NotTooOldValidator implements ConstraintValidator<NotTooOld, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Null се обработва от @NotNull
        }
        // Датата трябва да е не по-стара от 2 дни преди днес
        return !value.isBefore(LocalDate.now().minusDays(2));
    }
}
