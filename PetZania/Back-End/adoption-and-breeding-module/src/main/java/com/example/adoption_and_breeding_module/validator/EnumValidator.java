package com.example.adoption_and_breeding_module.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class EnumValidator implements ConstraintValidator<ValidEnum, Object> {

    private Class<? extends Enum<?>> enumClass;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;   // usual Bean Validation null‐is‐ok semantics
        }

        String name = value.toString();  // for enums this is the enum name
        boolean isValid = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .anyMatch(enumValue -> enumValue.equalsIgnoreCase(name));

        if (!isValid) {
            String accepted = Arrays.toString(enumClass.getEnumConstants());
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid Value. Accepted Values are: " + accepted
            ).addConstraintViolation();
        }
        return isValid;
    }
}

