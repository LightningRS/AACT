package com.iscas.aact.testcase.provider;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.Constants;
import com.iscas.aact.utils.CompModel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@Slf4j
public abstract class BaseValueProvider {
    protected String name;
    protected CompModel compModel;

    public String getName() {
        return this.name;
    }

    public String getPackageName() {
        return compModel.getPackageName();
    }

    public String getCompName() {
        return compModel.getClassName();
    }

    public abstract JSONObject getValueSet();

    public static JSONArray filterEmptyStrInJsonArray(@NotNull JSONArray jsonArray) {
        if (jsonArray.size() == 0 || !(jsonArray.get(0) instanceof String)) {
            return jsonArray;
        }
        JSONArray result = new JSONArray();
        jsonArray.stream().parallel().forEach(item -> {
            if (((String) item).isBlank()) {
                return;
            }
            synchronized (result) {
                result.add(item);
            }
        });
        return result;
    }

    public static void mergeCompJSONArrRecur(@NotNull JSONArray merged, JSONArray src) {
        if (src == null) {
            return;
        }
        src = filterEmptyStrInJsonArray(src);
        if (src.size() == 0) {
            return;
        }
        if (merged.size() == 0) {
            merged.addAll(src);
            return;
        }
        Type mergedType = merged.get(0).getClass();
        if (!mergedType.equals(src.get(0).getClass())) {
            throw new IllegalArgumentException(String.format(
                    "Cannot merge JSONArray<%s> to JSONArray<%s>",
                    src.get(0).getClass().getName(), mergedType.getTypeName()
            ));
        }
        if (JSONObject.class.equals(mergedType)) {
            // Check name first
            for (JSONObject srcObj : src.toJavaList(JSONObject.class)) {
                boolean flag = false;
                for (JSONObject orgObj : merged.toJavaList(JSONObject.class)) {
                    if (orgObj.getString("name").equals(srcObj.getString("name"))) {
                        // Name equals, merge values
                        if (srcObj.getJSONArray("values") != null) {
                            JSONArray orgValues = orgObj.getJSONArray("values");
                            if (orgValues == null) {
                                orgValues = new JSONArray();
                                orgObj.put("values", orgValues);
                            }
                            mergeCompJSONArrRecur(orgValues, srcObj.getJSONArray("values"));
                        }

                        // Merge body
                        if (srcObj.getJSONArray("body") != null) {
                            JSONArray orgBody = orgObj.getJSONArray("body");
                            if (orgBody == null) {
                                orgBody = new JSONArray();
                                orgObj.put("body", orgBody);
                            }
                            mergeCompJSONArrRecur(orgBody, srcObj.getJSONArray("body"));
                        }
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    merged.add(srcObj);
                }
            }
        } else {
            // Other types (including JSONArray)
            for (Object objToMerge : src) {
                if (objToMerge instanceof String) {
                    if (Constants.VAL_NOT_EMPTY.equals(objToMerge)) {
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
