package com.iscas.aact.logcat.parser;

import com.iscas.aact.logcat.annotations.LogcatParser;
import com.iscas.aact.logcat.interfaces.ILogcatParser;
import com.iscas.aact.logcat.utils.LogInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@LogcatParser(verbosity = ILogcatParser.LOGCAT_V_YEAR)
public class YearLogParser implements ILogcatParser {
    private static final Logger Log = LoggerFactory.getLogger(YearLogParser.class);
    private static final String LOGCAT_REGEXP_YEAR = "(?<time>[\\d-]+\\s[\\d:.]+)\\s+" +
            "(?<pid>\\d+)\\s+(?<tid>\\d+)\\s+(?<level>[A-Z])\\s+(?<tag>\\S*)\\s*:\\s";

    public LogInfo parse(String logLine) {
        LogInfo res = new LogInfo();
        Pattern pattern = Pattern.compile(LOGCAT_REGEXP_YEAR);
        Matcher matcher = pattern.matcher(logLine);
        if (!matcher.find() || matcher.groupCount() != 5) {
            Log.warn("Unknown log line format: {}", logLine);
            res.original = logLine;
            return res;
        }
        res.time = matcher.group("time");
        res.pid = matcher.group("pid");
        res.tid = matcher.group("tid");
        res.level = matcher.group("level");
        res.tag = matcher.group("tag");
        res.msg = matcher.replaceFirst("");
        res.original = logLine;
        return res;
    }
}
