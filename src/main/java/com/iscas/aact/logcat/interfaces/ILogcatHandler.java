package com.iscas.aact.logcat.interfaces;

import com.iscas.aact.logcat.LogcatMonitor;
import com.iscas.aact.logcat.annotations.LogcatHandler;
import com.iscas.aact.logcat.utils.LogInfo;

public interface ILogcatHandler extends Comparable<ILogcatHandler> {
    void handle(LogcatMonitor monitor, LogInfo logInfo);

    @Override
    default int compareTo(ILogcatHandler o) {
        LogcatHandler thisInfo = this.getClass().getAnnotation(LogcatHandler.class);
        LogcatHandler otherInfo = o.getClass().getAnnotation(LogcatHandler.class);
        if (thisInfo.priority() > otherInfo.priority()) {
            return 1;
        } else if (thisInfo.priority() < otherInfo.priority()) {
            return -1;
        } else {
            return thisInfo.name().compareTo(otherInfo.name());
        }
    }
}
