package com.iscas.aact.logcat.annotations;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogcatHandler {
    String name();

    int priority() default 10;

    String regex() default "";

    String[] keywords() default {};

    String tag() default "";
}
