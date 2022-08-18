package com.iscas.aact.logcat.interfaces;

import com.iscas.aact.logcat.utils.LogInfo;

public interface ILogcatParser {
    public static final String LOGCAT_V_YEAR = "year";

    LogInfo parse(String logLine);
}
