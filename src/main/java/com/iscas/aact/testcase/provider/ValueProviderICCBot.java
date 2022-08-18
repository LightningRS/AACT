package com.iscas.aact.testcase.provider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.testcase.ScopeConfigUtil;

import java.util.HashSet;
import java.util.Set;

public class ValueProviderICCBot extends ValueProvider {
    private final JSONObject fullValueSet;
    private final ScopeConfigUtil scopeCfg;

    public ValueProviderICCBot(JSONObject fullValueSet, ScopeConfigUtil scopeCfg) {
        super();
        this.name = "iccBot";
        this.fullValueSet = fullValueSet;
        this.scopeCfg = scopeCfg;
    }

    private static String getRealFieldName(String fieldName) {
        return switch (fieldName) {
            case "actions" -> "action";
            case "categories" -> "category";
            case "datas" -> "data";
            case "schemes" -> "scheme";
            case "hosts" -> "host";
            case "ports" -> "port";
            case "paths" -> "path";
            case "extras" -> "extra";
            case "flags" -> "flag";
            case "types" -> "type";
            default -> "unknown";
        };
    }

    public JSONObject getValueSet() {
        JSONObject mergedValueSetObj = new JSONObject();
        if (fullValueSet == null) return null;
        JSONObject fullValueSetDup = JSON.parseObject(fullValueSet.toJSONString());
        for (String fieldName : fullValueSetDup.keySet()) {
            String realFieldName = getRealFieldName(fieldName);
            JSONObject fieldValues = fullValueSetDup.getJSONObject(fieldName);

            if (realFieldName.equals("data")) {
                Set<String> hostsTemp = new HashSet<>();
                Set<String> portsTemp = new HashSet<>();
                for (String dataFieldName : fieldValues.keySet()) {
                    JSONObject dataFieldValues = fieldValues.getJSONObject(dataFieldName);
                    String realDataFieldName = getRealFieldName(dataFieldName);
                    JSONArray mergedValues = new JSONArray();
                    for (String scopeName : dataFieldValues.keySet()) {
                        if (!scopeCfg.getScopeConfig("data", scopeName)) continue;

                        if (realDataFieldName.equals("host")) {
                            hostsTemp.addAll(dataFieldValues.getJSONArray(scopeName).toJavaList(String.class));
                        } else if (realDataFieldName.equals("port")) {
                            portsTemp.addAll(dataFieldValues.getJSONArray(scopeName).toJavaList(String.class));
                        } else {
                            mergeCompJSONArrRecur(mergedValues, dataFieldValues.getJSONArray(scopeName));
                        }
                    }
                    if (!realDataFieldName.equals("host") && !realDataFieldName.equals("port"))
                        mergedValueSetObj.put(realDataFieldName, mergedValues);
                }

                Set<String> combAuths = new HashSet<>();
                if (hostsTemp.size() > 0) {
                    hostsTemp.forEach(h -> {
                        if (!h.equals("")) {
                            combAuths.add(h);
                            if (portsTemp.size() > 0) {
                                portsTemp.forEach(p -> {
                                    if (!p.equals("")) combAuths.add(h + ":" + p);
                                });
                            }
                        }
                    });
                } else if (portsTemp.size() > 0) {
                    portsTemp.forEach(p -> {
                        if (!p.equals("")) combAuths.add(p);
                    });
                }
                JSONArray newAuthorityArr = new JSONArray();
                JSONArray oldAuthorityArr = mergedValueSetObj.getJSONArray("authority");
                if (oldAuthorityArr == null) {
                    oldAuthorityArr = new JSONArray();
                    mergedValueSetObj.put("authority", oldAuthorityArr);
                }
                newAuthorityArr.addAll(combAuths);
                mergeCompJSONArrRecur(oldAuthorityArr, newAuthorityArr);
            } else {
                JSONArray mergedValues = new JSONArray();
                for (String scopeName : fieldValues.keySet()) {
                    if (!scopeCfg.getScopeConfig(realFieldName, scopeName)) continue;
                    mergeCompJSONArrRecur(mergedValues, fieldValues.getJSONArray(scopeName));
                }
                mergedValueSetObj.put(realFieldName, mergedValues);
            }
        }
        return mergedValueSetObj;
    }
}
