package com.iscas.aact.testcase;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base32;

import java.nio.charset.StandardCharsets;

public abstract class BaseFlattener {
    private static final Base32 BASE32 = new Base32();

    protected static String b32encode(String str) {
        return BASE32.encodeToString(str.getBytes(StandardCharsets.UTF_8)).replaceAll("=", "");
    }

    public abstract int flatten(JSONObject jsonObj);
}
