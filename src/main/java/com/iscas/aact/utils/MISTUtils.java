package com.iscas.aact.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MISTUtils {
    /**
     * Get component MIST type name.
     *
     * @param compModel Component model
     */
    public static void checkCompMISTType(CompModel compModel) {
        if (compModel == null) {
            return;
        }
        JSONObject mistResult = Config.getInstance().getMISTResult();
        if (mistResult == null) {
            // MIST result is not specified, ignore
            return;
        }
        boolean isExported = compModel.getExported() != null && compModel.getExported();
        if (!mistResult.containsKey(compModel.getPackageName())) {
            return;
        }
        JSONObject pkgResult = mistResult.getJSONObject(compModel.getPackageName());
        if (!pkgResult.containsKey(compModel.getClassName())) {
            log.debug("Component [{}/{}] is not in MIST result", compModel.getPackageName(), compModel.getClassName());
            return;
        }
        compModel.setMistType(pkgResult.getString(compModel.getClassName()));
    }
}
