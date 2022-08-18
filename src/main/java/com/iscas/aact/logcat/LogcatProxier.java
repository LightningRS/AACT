package com.iscas.aact.logcat;

import com.iscas.aact.utils.ADBInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Queue;

public class LogcatProxier {
    protected static final Logger Log = LoggerFactory.getLogger(LogcatProxier.class);
    protected Queue<String> mLogQueue;
    protected String mVerbosity;

    public LogcatProxier(Queue<String> logQueue, String verbosity) {
        this.mLogQueue = logQueue;
        this.mVerbosity = verbosity;
    }

    public void readLogcat() {
        ADBInterface adb = ADBInterface.getInstance();
        Process logcatProcess = adb.getLogcatProcess(this.mVerbosity);
        BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
        String s;
        try {
            while ((s = reader.readLine()) != null) {
                this.mLogQueue.offer(s);
            }
        } catch (IOException e) {
            Log.error("IOException when reading logcat", e);
        }
        // Restart LogcatProxyThread
        this.start();
    }

    public void start() {
        Thread proxyThread = new Thread("LogcatProxyThread") {
            @Override
            public void run() {
                readLogcat();
            }
        };
        proxyThread.setDaemon(true);
        proxyThread.start();
        Log.info("Logcat proxy thread started");
    }
}
