package com.iscas.aact.testcase.provider;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.Constants;
import com.iscas.aact.utils.CompModel;

import java.util.Arrays;

public class ValueProviderPreset extends ValueProvider {
    public ValueProviderPreset(CompModel compModel) {
        super();
        this.name = "preset";
        this.compModel = compModel;
    }

    /**
     * <h3>About PRESET values:
     *
     * <p>For all preset values of each field defined as <i>Constants.PRESET_VALUES_XXX</i>, we
     * ensure that at least <b>the first one</b> is an <i>INVALID</i> value, and <b>the second one</b>
     * is a <i>VALID</i> value.
     * <p>For those fields with collected values under send/recv scope in the component model, since
     * they <b>have been truly used in program code</b>, we put all pre-defined preset values into the
     * combination testing model, while only one <i>INVALID</i> and one <i>VALID</i> values
     * will be used otherwise.
     *
     * @return Preset value set as JSONObject
     **/
    public JSONObject getValueSet() {
        JSONObject res = new JSONObject();

        JSONArray categoryArr = new JSONArray();
        if (compModel.hasFieldScopeValues("sendIntent", "category")
                || compModel.hasFieldScopeValues("recvIntent", "category")) {
            categoryArr.addAll(Arrays.asList(Constants.PRESET_VALUES_CATEGORY));
        } else {
            categoryArr.addAll(Arrays.asList(Constants.PRESET_VALUES_CATEGORY).subList(0, 1));
        }
        res.put("category", categoryArr);

        JSONArray typeArr = new JSONArray();
        if (compModel.hasFieldScopeValues("sendIntent", "type")
                || compModel.hasFieldScopeValues("recvIntent", "type")) {
            typeArr.addAll(Arrays.asList(Constants.PRESET_VALUES_TYPE));
        } else {
            typeArr.addAll(Arrays.asList(Constants.PRESET_VALUES_TYPE).subList(0, 1));
        }
        res.put("type", typeArr);

        JSONArray schemeArr = new JSONArray();
        if (compModel.hasFieldScopeValues("sendIntent", "data", "scheme")
                || compModel.hasFieldScopeValues("recvIntent", "data", "scheme")) {
            schemeArr.addAll(Arrays.asList(Constants.PRESET_VALUES_SCHEME));
        } else {
            schemeArr.addAll(Arrays.asList(Constants.PRESET_VALUES_SCHEME).subList(0, 1));
        }
        res.put("scheme", schemeArr);

        JSONArray authorityArr = new JSONArray();
        if (compModel.hasFieldScopeValues("sendIntent", "data", "host")
                || compModel.hasFieldScopeValues("recvIntent", "data", "host")
                || compModel.hasFieldScopeValues("sendIntent", "data", "port")
                || compModel.hasFieldScopeValues("recvIntent", "data", "port")) {
            authorityArr.addAll(Arrays.asList(Constants.PRESET_VALUES_AUTHORITY));
        } else {
            authorityArr.addAll(Arrays.asList(Constants.PRESET_VALUES_AUTHORITY).subList(0, 1));
        }
        res.put("authority", authorityArr);

        JSONArray pathArr = new JSONArray();
        if (compModel.hasFieldScopeValues("sendIntent", "data", "path")
                || compModel.hasFieldScopeValues("recvIntent", "data", "path")) {
            pathArr.addAll(Arrays.asList(Constants.PRESET_VALUES_PATH));
        } else {
            pathArr.addAll(Arrays.asList(Constants.PRESET_VALUES_PATH).subList(0, 1));
        }
        res.put("path", pathArr);
        return res;
    }
}
