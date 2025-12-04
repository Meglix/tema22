package com.example.demo.dtos.validators;


import com.example.demo.dtos.validators.annotation.ConsumptionLimit;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConsumptionValidator implements ConstraintValidator<ConsumptionLimit, Integer> {
    private int min;
    @Override public void initialize(ConsumptionLimit ann) { this.min = ann.value(); }
    @Override public boolean isValid(Integer consumption, ConstraintValidatorContext ctx) {
        if (consumption == null) return true;               // let @NotNull enforce presence
        return consumption >= min;
    }
}


