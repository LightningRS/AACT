package com.iscas.aact.logcat.handler;

import com.iscas.aact.logcat.LogcatMonitor;
import com.iscas.aact.logcat.annotations.LogcatHandler;
import com.iscas.aact.logcat.interfaces.ILogcatHandler;
import com.iscas.aact.logcat.utils.LogInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@LogcatHandler(name = "RPCStartHandler", keywords = {"Test RPCServer started at port"})
public class RPCStartHandler implements ILogcatHandler {
    private static final Logger Log = LoggerFactory.getLogger(RPCStartHandler.class);

    @Override
    public void handle(LogcatMonitor monitor, LogInfo logInfo) {
        if (logInfo.msg == null) return;
        Pattern pattern = Pattern.compile("Test RPCServer started at port (?<port>\\d+)");
        Matcher matcher = pattern.matcher(logInfo.msg);
        if (!matcher.find()) {
            Log.error("Failed to match rpc remote port! logInfo.msg: {}", logInfo.msg);
            return;
        }
        Integer port = Integer.parseInt(matcher.group("port"));
        Log.info("Detected rpc remote port {}", port);
        monitor.getTestController().initRPC(port);
    }
}
