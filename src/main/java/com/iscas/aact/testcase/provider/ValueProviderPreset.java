package com.iscas.aact.testcase.provider;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.Constants;

import java.util.Arrays;

public class ValueProviderPreset extends ValueProvider {
    public ValueProviderPreset() {
        super();
        this.name = "preset";
    }

    public JSONObject getValueSet() {
        JSONObject res = new JSONObject();

        JSONArray categoryArr = new JSONArray();
        categoryArr.addAll(Arrays.asList(Constants.PRESET_VALUES_CATEGORY));
        res.put("category", categoryArr);

        JSONArray typeArr = new JSONArray();
        typeArr.addAll(Arrays.asList(Constants.PRESET_VALUES_TYPE));
        res.put("type", typeArr);

        JSONArray schemeArr = new JSONArray();
        schemeArr.addAll(Arrays.asList(Constants.PRESET_VALUES_SCHEME));
        res.put("scheme", schemeArr);

        JSONArray authorityArr = new JSONArray();
        authorityArr.addAll(Arrays.asList(Constants.PRESET_VALUES_AUTHORITY));
        res.put("authority", authorityArr);

        JSONArray pathArr = new JSONArray();
        pathArr.addAll(Arrays.asList(Constants.PRESET_VALUES_PATH));
        res.put("path", pathArr);
        return res;
    }
}
