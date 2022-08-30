package com.iscas.aact.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@AllArgsConstructor
public class CompModel {
    public static final String TYPE_ACTIVITY = "a";
    public static final String TYPE_SERVICE = "s";
    public static final String TYPE_RECEIVER = "r";

    private String packageName;
    private String className;
    private String type;
    private JSONObject compJson;

    /**
     * Whether component is enabled.
     * Will be null if not determined.
     */
    @Nullable
    private Boolean enabled;

    /**
     * Whether component is exported.
     * Will be null if not determined.
     */
    @Nullable
    private Boolean exported;

    /**
     * Component type in MIST result.
     * Will be null if not determined.
     */
    @Nullable
    private String mistType;

    public CompModel(String packageName, JSONObject compJson) {
        this(packageName, compJson.getString("className"), compJson.getString("type"), compJson, null, null, null);
    }

    public boolean hasFieldScopeValues(String scopeName, String... fieldNames) {
        JSONObject fieldValueSet = compJson.getJSONObject("fullValueSet");
        if (fieldValueSet == null) {
            return false;
        }
        for (String fieldName : fieldNames) {
            fieldValueSet = fieldValueSet.getJSONObject(ICCBotUtils.getModelFieldName(fieldName));
            if (fieldValueSet == null) {
                return false;
            }
        }
        return fieldValueSet.containsKey(scopeName);
    }
}
