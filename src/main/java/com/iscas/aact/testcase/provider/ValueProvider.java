package com.iscas.aact.testcase.provider;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.Constants;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public abstract class ValueProvider {
    String name;

    public ValueProvider() {
        this.name = "default";
    }

    public String getName() {
        return this.name;
    }

    public abstract JSONObject getValueSet();

    public static void mergeCompJSONArrRecur(@NotNull JSONArray merged, JSONArray src) {
        if (src == null || src.size() == 0) return;
        if (merged.size() == 0) {
            merged.addAll(src);
            return;
        }
        Object mergedFirstObj = merged.get(0);
        Class<?> srcObjType = src.get(0).getClass();
        if (!srcObjType.isInstance(mergedFirstObj)) {
            throw new IllegalArgumentException(String.format(
                    "Cannot merge JSONArray<%s> to JSONArray<%s>",
                    srcObjType.getName(), mergedFirstObj.getClass().getName()
            ));
        }
        if (mergedFirstObj.getClass() == JSONObject.class) {
            // Check name first
            for (JSONObject srcObj : src.toJavaList(JSONObject.class)) {
                boolean flag = false;
                for (JSONObject orgObj : merged.toJavaList(JSONObject.class)) {
                    if (orgObj.getString("name").equals(srcObj.getString("name"))) {
                        // Name equals, merge values
                        JSONArray orgValues = orgObj.getJSONArray("values");
                        if (orgValues == null) {
                            orgObj.put("values", srcObj.getJSONArray("values"));
                        } else {
                            mergeCompJSONArrRecur(orgValues, srcObj.getJSONArray("values"));
                        }

                        // Merge body
                        JSONArray orgBody = orgObj.getJSONArray("body");
                        if (orgBody == null) {
                            orgObj.put("body", srcObj.getJSONArray("body"));
                        } else {
                            mergeCompJSONArrRecur(orgBody, srcObj.getJSONArray("body"));
                        }
                        flag = true;
                        break;
                    }
                }
                if (!flag) merged.add(srcObj);
            }
        } else {
            // Other types (including JSONArray)
            for (Object objToMerge : src) {
                if (objToMerge instanceof String) {
                    if (objToMerge.equals(Constants.VAL_NOT_EMPTY)) {
                        log.debug("Detected notEmpty value");
                    } else if (!merged.contains(objToMerge)) {
                        merged.add(objToMerge);
                    }
                } else if (!merged.contains(objToMerge)) {
                    merged.add(objToMerge);
                }
            }
        }
    }
}
