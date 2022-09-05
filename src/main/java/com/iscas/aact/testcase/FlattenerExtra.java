package com.iscas.aact.testcase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.Constants;
import com.iscas.aact.utils.Config;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class FlattenerExtra extends BaseFlattener {
    private static final Logger Log = LoggerFactory.getLogger(FlattenerExtra.class);
    private static int nodeId;

    @Override
    public int flatten(JSONObject valueSet) {
        JSONArray extraArr = valueSet.getJSONArray("extra");
        JSONArray extraBaseValues = new JSONArray();
        extraBaseValues.add(Constants.VAL_EMPTY);
        if (extraArr == null) {
            valueSet.put("extra", extraBaseValues);
            return 0;
        }
        nodeId = 0;
        int sum = 0;
        for (JSONObject extraObj : extraArr.toJavaList(JSONObject.class)) {
            sum += flattenRecur(valueSet, extraObj, 0);
        }
        extraBaseValues.add(Constants.VAL_NOT_EMPTY);
        valueSet.put("extra", extraBaseValues);
        return sum;
    }

    public static String encodeExtraName(String name) {
        return b32encode(name);
    }

    public static String encodeExtraNameAndType(String name, String type) {
        if (type.isBlank() || name.isBlank()) {
            log.error("Extra name and type cannot be empty! name={}, type={}", name, type);
            return null;
        }
        type = type.replaceAll("java\\.lang\\.", "");
        if (type.contains("@")) {
            type = type.substring(0, type.indexOf("@"));
        }
        return b32encode(name) + "_" + b32encode(type);
    }

    @Nullable
    public static String encodeExtraNameAndType(String nameAndType) {
        if (!nameAndType.contains("-")) {
            log.error("Invalid extra nameAndType format: {}", nameAndType);
            return null;
        }
        String[] sp = nameAndType.split("-", 2);
        String type = sp[0].trim(), name = sp[1].trim();
        return encodeExtraNameAndType(name, type);
    }

    private static void appendBoundaryValues(Set<String> fValues, String extraType) {
        switch (extraType.toLowerCase()) {
            case "boolean" -> fValues.addAll(Arrays.asList("true", "false"));
            case "char" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Character.MIN_VALUE),
                    String.valueOf(Character.MAX_VALUE)
            ));
            case "byte" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Byte.MIN_VALUE),
                    String.valueOf(Byte.MAX_VALUE),
                    "0"
            ));
            case "short" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Short.MIN_VALUE),
                    String.valueOf(Short.MAX_VALUE),
                    "0"
            ));
            case "int" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Integer.MAX_VALUE),
                    String.valueOf(Integer.MIN_VALUE),
                    "0"
            ));
            case "long" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Long.MAX_VALUE),
                    String.valueOf(Long.MIN_VALUE),
                    "0"
            ));
            case "float" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Float.MAX_VALUE),
                    String.valueOf(Float.MIN_VALUE),
                    "0"
            ));
            case "double" -> fValues.addAll(Arrays.asList(
                    String.valueOf(Double.MAX_VALUE),
                    String.valueOf(Double.MIN_VALUE),
                    "0"
            ));
            default -> fValues.addAll(Arrays.asList(
                    Constants.VAL_NULL, Constants.VAL_EMPTY
            ));
        }
    }

    private int flattenRecur(JSONObject valueSet, JSONObject extraObj, int parentId) {
        nodeId++;
        int thisNodeId = nodeId;
        String extraName = extraObj.getString("name");
        String extraType = extraObj.getString("type").replaceAll("java\\.lang\\.", "");
        String extraNodeName = "extra_" + parentId + "_" + nodeId + "_" + encodeExtraNameAndType(extraName, extraType);
        Set<String> fValues = new HashSet<>();
        JSONArray bodyObj = extraObj.getJSONArray("body");
        if (bodyObj == null) {
            // Leaf node
            JSONArray values = extraObj.getJSONArray("values");
            if (values != null) {
                for (String value : values.toJavaList(String.class)) {
                    if (value.equals(Constants.VAL_NOT_EMPTY)) {
                        Log.debug("Detected notEmpty value");
                    } else if ("boolean".equalsIgnoreCase(extraType)) {
                        fValues.add(String.valueOf(Boolean.valueOf(value)));
                    } else {
                        fValues.add(value);
                    }
                }
                if (Config.getInstance().getAppendBoundaryValues()) {
                    appendBoundaryValues(fValues, extraType);
                }
            } else {
                appendBoundaryValues(fValues, extraType);
            }
            valueSet.put(extraNodeName, fValues);
            return 1;
        } else {
            fValues.addAll(Arrays.asList(Constants.VAL_NULL, Constants.VAL_EMPTY, Constants.VAL_NOT_EMPTY));
            valueSet.put(extraNodeName, fValues);
            int sum = 0;
            for (JSONObject subObj : bodyObj.toJavaList(JSONObject.class)) {
                sum += flattenRecur(valueSet, subObj, thisNodeId);
            }
            return 1 + sum;
        }
    }
}
