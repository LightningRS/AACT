package com.iscas.aact;

public class Constants {
    public static final String CLIENT_PKG_NAME = "com.test.apptestclient";
    public static final String CLIENT_ACT_NAME = CLIENT_PKG_NAME + ".activities.MainActivity";
    public static final String CLIENT_COMP_NAME = CLIENT_PKG_NAME + "/.activities.MainActivity";
    public static final String CLIENT_CASE_ROOT = "/sdcard/ICCBot/testcases";

    public static final String VAL_NULL = "$#NULL#$";
    public static final String VAL_EMPTY = "$#EMPTY#$";
    public static final String VAL_NOT_EMPTY = "$#NOTEMPTY#$";

    public static final long TIMEOUT_START_MS = 5000;
    public static final long TIME_DISPLAY_REQUIRE_MS = 1000;
    public static final long COMP_CHECK_INTERVAL = 200;

    public static final int DEFAULT_RAND_VAL_NUM = 5;
    public static final int DEFAULT_RAND_STR_MIN_LENGTH = 1;
    public static final int DEFAULT_RAND_STR_MAX_LENGTH = 20;

    public static final String DEFAULT_SCOPE_CONFIG = """
            ,manifest,sendIntent,recvIntent,specIntent
            action,1,1,1,1
            category,1,1,1,1
            data,1,1,1,1
            extra,0,1,1,0
            flag,0,1,1,1
            type,1,1,1,1
            """;

    public static final String[] PRESET_VALUES_CATEGORY = {
            "android.intent.category.DEFAULT",
            "android.intent.category.LAUNCHER",
            "android.intent.category.TEST",
            "android.intent.category.UNIT_TEST",
            "android.intent.category.SAMPLE_CODE"
    };

    /* Reference: https://stackoverflow.com/a/24134677 */
    public static final String[] PRESET_VALUES_TYPE = {
            "image/jpeg", "image/png", "image/gif",
            "text/plain", "text/html", "text/xml",
            "audio/mpeg", "audio/aac", "audio/wav", "audio/ogg", "audio/midi",
            "video/mp4", "video/x-msvideo", "video/x-ms-wmv",
            "application/pdf", "application/vnd.android.package-archive"
    };

    public static final String[] PRESET_VALUES_SCHEME = {
            "http", "https", "mailto", "ftp", "tel", "sms", "geo"
    };

    public static final String[] PRESET_VALUES_AUTHORITY = {
            "localhost", "127.0.0.1", "12345678", "0,0"
    };

    public static final String[] PRESET_VALUES_PATH = {
            "/", "/data", "/storage/emulated/0", "/sdcard"
    };
}
