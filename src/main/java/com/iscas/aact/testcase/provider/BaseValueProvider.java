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

    /**
     * 获取生成的取值集合
     * @return 取值集合
     */
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
        Type srcType = src.get(0).getClass();
        if (merged.size() > 0) {
            Type mergedType = merged.get(0).getClass();
            if (!mergedType.equals(srcType)) {
                throw new IllegalArgumentException(String.format(
                        "Cannot merge JSONArray<%s> to JSONArray<%s>", srcType, mergedType
                ));
            }
        }
        if (JSONObject.class.equals(srcType)) {
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
                    if ("notEmpty".equals(objToMerge)) {
                        log.debug("Detected notEmpty value");
                        merged.add(Constants.VAL_NOT_EMPTY);
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
