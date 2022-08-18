package com.iscas.aact.logcat.handler;

import com.iscas.aact.logcat.LogcatMonitor;
import com.iscas.aact.logcat.annotations.LogcatHandler;
import com.iscas.aact.logcat.interfaces.ILogcatHandler;
import com.iscas.aact.logcat.utils.LogInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LogcatHandler(name = "BeginOfCrashHandler", regex = "^[-\\s]+beginning of crash", priority = 20)
public class BeginOfCrashHandler implements ILogcatHandler {
    @Override
    public void handle(LogcatMonitor monitor, LogInfo logInfo) {
        monitor.getTestController().getCompStateMonitor().onBeginOfCrash();
    }
}
