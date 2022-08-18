package com.iscas.aact.logcat;

import com.iscas.aact.logcat.annotations.LogcatParser;
import com.iscas.aact.logcat.interfaces.ILogcatParser;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LogcatParserFactory {
    private static final Logger Log = LoggerFactory.getLogger(LogcatParserFactory.class);
    private static final Map<String, ILogcatParser> sParserClasses = new ConcurrentHashMap<>();
    private static volatile boolean sIsInit = false;

    private static class ParserFactoryInit {
        private static final boolean sIsInit = loadParserClasses();
    }

    private static boolean loadParserClasses() {
        Reflections ref = new Reflections(LogcatParserFactory.class.getPackageName() + ".parser");
        Set<Class<? extends ILogcatParser>> classes = ref.getSubTypesOf(ILogcatParser.class);
        for (Class<? extends ILogcatParser> clazz : classes) {
            LogcatParser parserInfo = clazz.getAnnotation(LogcatParser.class);
            try {
                sParserClasses.put(parserInfo.verbosity(), clazz.getDeclaredConstructor().newInstance());
                Log.debug("Registered logcat parser [{}] for verbosity [{}]", clazz.getName(), parserInfo.verbosity());
            } catch (ReflectiveOperationException e) {
                Log.error("Failed to create parser for class [{}]", clazz.getName(), e);
            }
        }
        return true;
    }

    public static ILogcatParser getParser(String verbosity) {
        if (!sIsInit) {
            synchronized (LogcatParserFactory.class) {
                if (!sIsInit) {
                    loadParserClasses();
                    sIsInit = true;
                }
            }
        }
        if (!sParserClasses.containsKey(verbosity)) {
            Log.error("No parser found for logcat verbosity: {}", verbosity);
            return null;
        }
        return sParserClasses.get(verbosity);
    }
}
