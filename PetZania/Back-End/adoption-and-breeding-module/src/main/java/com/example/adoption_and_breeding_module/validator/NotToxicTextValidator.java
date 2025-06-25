package com.example.adoption_and_breeding_module.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.adoption_and_breeding_module.service.impl.TextToxicityChecker;

@Component
public class NotToxicTextValidator implements ConstraintValidator<NotToxicText, String> {

    private final TextToxicityChecker textToxicityChecker;

    @Autowired
    public NotToxicTextValidator(TextToxicityChecker textToxicityChecker) {
        this.textToxicityChecker = textToxicityChecker;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotBlank/@NotNull handle this
        }
        try {
            return !textToxicityChecker.isToxic(value);
        }
        catch (Exception e) {
            e.printStackTrace();
            return true; // or false if you prefer to reject in case of model failure
        }
    }
}
