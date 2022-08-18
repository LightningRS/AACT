package com.iscas.aact.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CompModel {
    public static final String TYPE_ACTIVITY = "a";
    public static final String TYPE_SERVICE = "s";
    public static final String TYPE_RECEIVER = "r";

    private String className;
    private String type;
}
