package com.iscas.aact.logcat.annotations;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogcatParser {
    String verbosity();
}
