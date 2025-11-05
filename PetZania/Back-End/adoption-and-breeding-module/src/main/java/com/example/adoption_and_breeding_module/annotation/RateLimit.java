package com.example.adoption_and_breeding_module.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.example.adoption_and_breeding_module.constant.Constants.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int requests() default RATE_LIMIT_DEFAULT_REQUESTS;

    int duration() default RATE_LIMIT_DEFAULT_DURATION;
}
