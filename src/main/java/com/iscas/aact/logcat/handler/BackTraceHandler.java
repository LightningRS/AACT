package com.iscas.aact.logcat.handler;

import com.iscas.aact.logcat.LogcatMonitor;
import com.iscas.aact.logcat.annotations.LogcatHandler;
import com.iscas.aact.logcat.interfaces.ILogcatHandler;
import com.iscas.aact.logcat.utils.LogInfo;

@LogcatHandler(name = "BackTraceHandler", regex = ".*", priority = 1)
public class BackTraceHandler implements ILogcatHandler {
    @Override
    public void handle(LogcatMonitor monitor, LogInfo logInfo) {
        StackTraceHandler stHandler = monitor.getHandlerByClass(StackTraceHandler.class);
        stHandler.setLastLineInfo(logInfo);
    }
}
