package com.example.auth.dtos.annotation;

import com.example.auth.dtos.annotation.validators.UsernameRestriction;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UsernameValidation implements ConstraintValidator<UsernameRestriction, String> {
    private String pattern;
    @Override
    public void initialize(UsernameRestriction constraintAnnotation) {
        this.pattern = constraintAnnotation.characters();
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext constraintValidatorContext) {
        if (username == null) {
            return true;
        }

        Pattern regexPattern = Pattern.compile(this.pattern);
        Matcher matcher = regexPattern.matcher(username);

        return matcher.matches();
    }

}
