package org.example.contract.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LanguageCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLanguageCode {
    String message() default "Некорректный код языка (например, 'ru', 'en')";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
