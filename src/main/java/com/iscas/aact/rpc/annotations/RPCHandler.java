package com.iscas.aact.rpc.annotations;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RPCHandler {
    String name();

    int priority() default 10;

    boolean autoRemove() default true;
}
