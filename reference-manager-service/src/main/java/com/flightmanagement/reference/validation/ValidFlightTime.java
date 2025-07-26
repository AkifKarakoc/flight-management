package com.flightmanagement.reference.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FlightTimeValidator.class)
@Documented
public @interface ValidFlightTime {
    String message() default "Arrival time must be after departure time";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}