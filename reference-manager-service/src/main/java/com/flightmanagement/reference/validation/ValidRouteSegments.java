package com.flightmanagement.reference.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RouteSegmentsValidator.class)
@Documented
public @interface ValidRouteSegments {
    String message() default "Route segments must be connected and properly ordered";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}