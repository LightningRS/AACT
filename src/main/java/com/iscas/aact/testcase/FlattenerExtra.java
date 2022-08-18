package com.iscas.aact.testcase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FlattenerExtra extends Flattener {
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

    private int flattenRecur(JSONObject valueSet, JSONObject extraObj, int parentId) {
        nodeId++;
        int thisNodeId = nodeId;
        String objType = extraObj.getString("type").replaceAll("java\\.lang\\.", "");
        String extraNodeName = "extra_" + parentId + "_" + nodeId + "_" +
                b32encode(extraObj.getString("name")) + "_" + b32encode(objType);
        Set<String> fValues = new HashSet<>();
        JSONArray bodyObj = extraObj.getJSONArray("body");
        if (bodyObj == null) {
            // Leaf node
            JSONArray values = extraObj.getJSONArray("values");
            if (values != null) {
                for (String value : values.toJavaList(String.class)) {
                    if (value.equals(Constants.VAL_NOT_EMPTY)) {
                        Log.debug("Detected notEmpty value");
                    } else if (objType.equalsIgnoreCase("boolean")) {
                        fValues.add(String.valueOf(Boolean.valueOf(value)));
                    } else {
                        fValues.add(value);
                    }
                }
            } else {
                switch (objType) {
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
