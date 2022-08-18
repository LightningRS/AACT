package com.iscas.aact.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class CompModelUtil {
    private static final Logger Log = LoggerFactory.getLogger(CompModelUtil.class);
    private final String mPkgName;
    private final String mModelVersion;
    private final SortedMap<String, JSONObject> mComponentsMap;
    private List<JSONObject> mComponentsList;

    public CompModelUtil(JSONObject compModelObj) {
        mPkgName = compModelObj.getString("package");
        if (mPkgName == null) {
            throw new IllegalArgumentException("[package] field not found in component model");
        }
        mModelVersion = compModelObj.getString("version");
        if (mModelVersion == null) {
            throw new IllegalArgumentException("[version] field not found in component model, package=" + mPkgName);
        }
        mComponentsMap = new TreeMap<>();
        JSONArray compJsonArr = compModelObj.getJSONArray("components");
        if (compJsonArr == null) {
            Log.warn("[components] field not found in component model, package=" + mPkgName + ", version=" + mModelVersion);
        } else {
            List<JSONObject> compArr = compJsonArr.toJavaList(JSONObject.class);
            for (JSONObject comp : compArr) {
                mComponentsMap.put(comp.getString("className"), comp);
            }
        }
        mComponentsList = mComponentsMap.values().stream().toList();
    }

    public List<String> getAllCompNames() {
        return mComponentsMap.keySet().stream().toList();
    }

    public String getPackageName() {
        return mPkgName;
    }

    public String getModelVersion() {
        return mModelVersion;
    }

    public JSONObject getCompModelJSONByIndex(int index) {
        return mComponentsList.get(index);
    }

    public CompModel getCompModelByIndex(int index) {
        JSONObject compJson = getCompModelJSONByIndex(index);
        if (compJson == null) return null;
        return new CompModel(compJson.getString("className"), compJson.getString("type"));
    }

    public String getCompNameByIndex(int index) {
        CompModel compJson = getCompModelByIndex(index);
        if (compJson == null) return null;
        return compJson.getClassName();
    }

    public String getCompTypeByIndex(int index) {
        CompModel compJson = getCompModelByIndex(index);
        if (compJson == null) return null;
        return compJson.getType();
    }

    public int getCompCount() {
        return mComponentsMap.size();
    }

    public void removeComps(List<String> compNames) {
        for (String compName : compNames) {
            mComponentsMap.remove(compName);
        }
        mComponentsList = mComponentsMap.values().stream().toList();
        Log.debug("Removed {} components", compNames.size());
    }
}
