package com.iscas.aact.logcat;

import com.iscas.aact.TestController;
import com.iscas.aact.logcat.annotations.LogcatHandler;
import com.iscas.aact.logcat.interfaces.ILogcatHandler;
import com.iscas.aact.logcat.interfaces.ILogcatParser;
import com.iscas.aact.logcat.utils.LogInfo;
import com.iscas.aact.utils.SortedArrayList;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogcatMonitor {
    private static final Logger Log = LoggerFactory.getLogger(LogcatMonitor.class);

    private final TestController mTestController;
    private final BlockingQueue<String> mLogQueue;
    private final LogcatProxier mLogcatProxier;
    private final SortedArrayList<ILogcatHandler> mHandlerList;
    private final ILogcatParser mLogcatParser;

    public LogcatMonitor(TestController testController) {
        this(testController, ILogcatParser.LOGCAT_V_YEAR);
    }

    public LogcatMonitor(TestController testController, String logcatVerbosity) {
        mTestController = testController;
        mLogcatParser = LogcatParserFactory.getParser(logcatVerbosity);
        mLogQueue = new LinkedBlockingQueue<>();
        mLogcatProxier = new LogcatProxier(mLogQueue, logcatVerbosity);
        mHandlerList = new SortedArrayList<>();
        registerHandlers();
    }

    public TestController getTestController() {
        return mTestController;
    }

    private void registerHandlers() {
        mHandlerList.clear();
        Reflections ref = new Reflections(getClass().getPackageName() + ".handler");
        Set<Class<?>> classes = ref.getTypesAnnotatedWith(LogcatHandler.class);
        for (Class<?> clazz : classes) {
            if (!ILogcatHandler.class.isAssignableFrom(clazz)) {
                Log.error("Handler [{}] is not implements ILogcatHandler", clazz.getName());
                return;
            }
            LogcatHandler handlerConfig = clazz.getAnnotation(LogcatHandler.class);
            try {
                mHandlerList.add((ILogcatHandler) clazz.getDeclaredConstructor().newInstance());
                Log.debug("Registered logcat handler [{}]", handlerConfig.name());
            } catch (ReflectiveOperationException e) {
                Log.error("Cannot construct handler on class [{}]", clazz.getName(), e);
            }
        }
        Collections.reverse(mHandlerList);
    }

    public ILogcatHandler getHandlerByName(String handlerName) {
        for (ILogcatHandler handler : mHandlerList) {
            LogcatHandler handlerInfo = handler.getClass().getAnnotation(LogcatHandler.class);
            if (handlerInfo.name().equals(handlerName)) return handler;
        }
        return null;
    }

    public <T> T getHandlerByClass(Class<T> handlerClass) {
        for (ILogcatHandler handler : mHandlerList) {
            if (handlerClass.isInstance(handler)) {
                return handlerClass.cast(handler);
            }
        }
        return null;
    }

    private boolean checkHandlerConditions(LogcatHandler handlerInfo, LogInfo logInfo) {
        if (!handlerInfo.regex().equals("")) {
            Pattern pattern = Pattern.compile(handlerInfo.regex());
            Matcher matcher = pattern.matcher(logInfo.original);
            if (!matcher.find()) return false;
        }
        if (!handlerInfo.tag().equals("")) {
            if (logInfo.tag == null) return false;
            if (!logInfo.tag.equals(handlerInfo.tag())) return false;
        }
        if (handlerInfo.keywords().length > 0) {
            for (String keyword : handlerInfo.keywords()) {
                if (!logInfo.original.contains(keyword)) return false;
            }
        }
        return true;
    }

    private void handle() {
        while (true) {
            try {
                String logLine = mLogQueue.take();
                Log.debug("logLine: {}", logLine);
                LogInfo res = this.mLogcatParser.parse(logLine);
                if (res != null) {
                    for (ILogcatHandler handler : mHandlerList) {
                        LogcatHandler handlerInfo = handler.getClass().getAnnotation(LogcatHandler.class);
                        if (checkHandlerConditions(handlerInfo, res)) {
                            // Log.debug("Call logcat handler [{}] ({})", handlerInfo.name(), handler.getClass().getName());
                            handler.handle(this, res);
                        }
                    }
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void start() {
        mLogcatProxier.start();
        Thread handleThread = new Thread("LogcatHandleThread") {
            @Override
            public void run() {
                handle();
            }
        };
        handleThread.setDaemon(true);
        handleThread.start();
        Log.info("Logcat handle thread start");
    }
}
