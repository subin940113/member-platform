package com.example.member.presentation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class KoreanMobilePhoneValidator implements ConstraintValidator<KoreanMobilePhone, String> {

    private static final int MAX_RAW_LENGTH = 32;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        if (value.length() > MAX_RAW_LENGTH) {
            return false;
        }
        String normalized = value.replaceAll("\\D", "");
        if (normalized.length() < 10 || normalized.length() > 11) {
            return false;
        }
        return normalized.matches("^01[0-9]{8,9}$");
    }
}
