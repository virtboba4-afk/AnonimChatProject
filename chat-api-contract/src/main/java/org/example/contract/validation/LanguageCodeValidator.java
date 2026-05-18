package org.example.contract.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class LanguageCodeValidator implements ConstraintValidator<ValidLanguageCode, String> {
    private static final Pattern LANGUAGE_CODE_PATTERN = Pattern.compile("^[a-z]{2}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return true;
        return LANGUAGE_CODE_PATTERN.matcher(value.toLowerCase()).matches();
    }
}

