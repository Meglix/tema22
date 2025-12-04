package com.example.auth.dtos.annotation.validators;

import com.example.auth.dtos.annotation.UsernameValidation;
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
@Constraint(validatedBy = UsernameValidation.class)
public @interface UsernameRestriction{
    String characters () default "[a-zA-Z]+";
    String message() default "Username should only contain a-z A-Z letters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}