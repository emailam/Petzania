package com.example.notificationmodule.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.example.notificationmodule.constant.Constants.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int requests() default RATE_LIMIT_DEFAULT_REQUESTS;

    int duration() default RATE_LIMIT_DEFAULT_DURATION;
}
