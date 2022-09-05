package com.iscas.aact.logcat.handler;

import com.iscas.aact.logcat.LogcatMonitor;
import com.iscas.aact.logcat.annotations.LogcatHandler;
import com.iscas.aact.logcat.interfaces.ILogcatHandler;
import com.iscas.aact.logcat.utils.LogInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@LogcatHandler(name = "ActivityDisplayedHandler", regex = "I/ActivityManager: Displayed", priority = 1)
public class ActivityDisplayedHandler implements ILogcatHandler {
    private static final Pattern PATTERN = Pattern.compile("Displayed (?<compName>[^:]+): (?<startDelay>.*)");
    @Override
    public void handle(LogcatMonitor monitor, LogInfo logInfo) {
        if (logInfo.msg == null) {
            return;
        }
        Matcher matcher = PATTERN.matcher(logInfo.msg);
        if (!matcher.find() || matcher.groupCount() != 2) {
            log.error("Activity displayed log pattern match failed: " + logInfo.msg);
            return;
        }
        String compName = matcher.group("compName");
        monitor.getTestController().getCompStateMonitor().onActivityDisplayed(compName);
    }
}
