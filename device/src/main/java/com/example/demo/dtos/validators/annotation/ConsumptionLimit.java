package com.example.demo.dtos.validators.annotation;

import com.example.demo.dtos.validators.ConsumptionValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE,
        ElementType.TYPE_USE,
        ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ConsumptionValidator.class)
public @interface ConsumptionLimit {
    int value() default 100;                        // min consumption
    String message() default "the consumption must be at least {value}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
