package com.iscas.aact.testcase;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base32;

import java.nio.charset.StandardCharsets;

public abstract class Flattener {
    private static final Base32 base32 = new Base32();

    protected String b32encode(String str) {
        return base32.encodeToString(str.getBytes(StandardCharsets.UTF_8)).replaceAll("=", "");
    }

    public abstract int flatten(JSONObject jsonObj);
}
