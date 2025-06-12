package com.example.adoption_and_breeding_module.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Class<? extends Enum<?>> enumClass;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        boolean isValid = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .anyMatch(enumValue -> enumValue.equalsIgnoreCase(value.replaceAll("\\s+", "_")));
        if (!isValid) {
            String acceptedValues = Arrays.toString(enumClass.getEnumConstants());

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid Value. Accepted Values are: " + acceptedValues
            ).addConstraintViolation();
        }

        return isValid;
    }
}
