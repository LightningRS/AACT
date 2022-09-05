package com.iscas.aact.testcase.provider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.Constants;
import com.iscas.aact.testcase.ScopeConfig;
import com.iscas.aact.utils.CompModel;
import com.iscas.aact.utils.ICCBotUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ValueProviderICCBot extends BaseValueProvider {
    private final JSONObject fullValueSet;
    private final ScopeConfig scopeCfg;

    public ValueProviderICCBot(CompModel compModel, JSONObject fullValueSet, ScopeConfig scopeCfg) {
        super();
        this.name = "iccBot";
        this.compModel = compModel;
        this.fullValueSet = fullValueSet;
        this.scopeCfg = new ScopeConfig(scopeCfg);
    }

    protected void updateScopeCfgByMIST() {
        if (Constants.MIST_TYPE_MUST_IA.equals(compModel.getMistType())) {
            log.info("MIST result of component [{}/{}] is mustIA, only consider send/recv scope",
                    getPackageName(), getCompName());
            scopeCfg.replaceScopeConfig("manifest", false);
            scopeCfg.replaceScopeConfig("specIntent", false);
        }
    }

    @Override
    public JSONObject getValueSet() {
        if (fullValueSet == null) {
            return null;
        }
        JSONObject mergedValueSetObj = new JSONObject();
        updateScopeCfgByMIST();
        JSONObject fullValueSetDup = JSON.parseObject(fullValueSet.toJSONString());
        for (String fieldName : fullValueSetDup.keySet()) {
            String realFieldName = ICCBotUtils.getRealFieldName(fieldName);
            JSONObject fieldValues = fullValueSetDup.getJSONObject(fieldName);

            if ("data".equals(realFieldName)) {
                Set<String> hostsTemp = new HashSet<>();
                Set<String> portsTemp = new HashSet<>();
                for (String dataFieldName : fieldValues.keySet()) {
                    JSONObject dataFieldValues = fieldValues.getJSONObject(dataFieldName);
                    String realDataFieldName = ICCBotUtils.getRealFieldName(dataFieldName);
                    JSONArray mergedValues = new JSONArray();
                    for (String scopeName : dataFieldValues.keySet()) {
                        if (!scopeCfg.getScopeConfig("data", scopeName)) {
                            continue;
                        }

                        if ("host".equals(realDataFieldName)) {
                            hostsTemp.addAll(dataFieldValues.getJSONArray(scopeName).toJavaList(String.class));
                        } else if ("port".equals(realDataFieldName)) {
                            portsTemp.addAll(dataFieldValues.getJSONArray(scopeName).toJavaList(String.class));
                        } else {
                            mergeCompJSONArrRecur(mergedValues, dataFieldValues.getJSONArray(scopeName));
                        }
                    }
                    if (!"host".equals(realDataFieldName) && !"port".equals(realDataFieldName)) {
                        mergedValueSetObj.put(realDataFieldName, mergedValues);
                    }
                }

                Set<String> combAuths = new HashSet<>();
                if (hostsTemp.size() > 0) {
                    hostsTemp.forEach(h -> {
                        if (!h.isBlank()) {
                            combAuths.add(h);
                            if (portsTemp.size() > 0) {
                                portsTemp.forEach(p -> {
                                    if (!"".equals(p.trim())) {
                                        combAuths.add(h + ":" + p);
                                    }
                                });
                            }
                        }
                    });
                } else if (portsTemp.size() > 0) {
                    portsTemp.forEach(p -> {
                        if (!p.trim().isBlank()) {
                            combAuths.add(p);
                        }
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
                    if (!scopeCfg.getScopeConfig(realFieldName, scopeName)) {
                        continue;
                    }
                    mergeCompJSONArrRecur(mergedValues, fieldValues.getJSONArray(scopeName));
                }
                mergedValueSetObj.put(realFieldName, mergedValues);
            }
        }
        return mergedValueSetObj;
    }
}
