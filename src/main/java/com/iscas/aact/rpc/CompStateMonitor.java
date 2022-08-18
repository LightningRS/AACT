package com.iscas.aact.rpc;

import com.iscas.aact.Constants;
import com.iscas.aact.TestController;
import com.iscas.aact.utils.ADBInterface;
import com.iscas.aact.utils.CompModel;
import com.iscas.aact.utils.Config;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CompStateMonitor {
    public static int STATE_UNK = 0;
    public static int STATE_DISPLAYED = 1;

    public static int STATE_RESULT = 2;

    public static int STATE_CLIENT_ERROR = 3;
    public static int STATE_INTENT_ERR = 4;
    public static int STATE_SYS_ERR = 5;
    public static int STATE_APP_CRASHED = 6;
    public static int STATE_TIMEOUT = 7;
    public static int STATE_DISPLAYED_TIMEOUT = 8;
    public static int STATE_JUMPED = 9;
    public static int STATE_SUCCESS = 10;

    private final ADBInterface adb = ADBInterface.getInstance();
    private final Config GlobalConfig = Config.getInstance();
    private final TestController testController;
    private String pkgName;
    private String compName;
    private String compType;
    private String focusedActivity;
    private Integer compState = STATE_UNK;
    private Long startedAt;
    private Long displayedAt;

    public CompStateMonitor(TestController testController) {
        this.testController = testController;
    }

    public void setCompState(Integer state) {
        if (state < STATE_UNK || state > STATE_SUCCESS) {
            throw new IllegalArgumentException("Invalid component state: " + state);
        }
        this.compState = state;
    }

    public Integer getCompState() {
        return compState;
    }

    public String getCompStateName() {
        if (compState.equals(STATE_UNK)) return "UNK";
        else if (compState.equals(STATE_DISPLAYED)) return "DISPLAYED";
        else if (compState.equals(STATE_CLIENT_ERROR)) return "CLIENT_ERROR";
        else if (compState.equals(STATE_INTENT_ERR)) return "INTENT_ERROR";
        else if (compState.equals(STATE_SYS_ERR)) return "SYS_ERROR";
        else if (compState.equals(STATE_APP_CRASHED)) return "APP_CRASHED";
        else if (compState.equals(STATE_TIMEOUT)) return "TIMEOUT";
        else if (compState.equals(STATE_DISPLAYED_TIMEOUT)) return "DISPLAYED_TIMEOUT";
        else if (compState.equals(STATE_JUMPED)) return "JUMPED";
        else if (compState.equals(STATE_SUCCESS)) return "SUCCESS";
        else return "INVALID";
    }

    public String getFocusedActivity() {
        return focusedActivity;
    }

    public void start() {
        this.startedAt = new Date().getTime();
        this.displayedAt = null;
        this.compState = STATE_UNK;
        log.info("Started component state monitor for compoent [" + compName + "]");
        new Thread("CompStateMonitor") {
            @Override
            public void run() {
                while (compState < STATE_RESULT) {
                    checkOnce();
                }
            }
        }.start();
    }

    private void checkOnce() {
        long st = new Date().getTime();
        // logcat is checking by ActivityDisplayedHandler
        // Check current focused activity
        checkByDumpSys();
        // Check timeout
        checkTimeout();
        while (new Date().getTime() - st < Constants.COMP_CHECK_INTERVAL) {
            Thread.yield();
        }
    }

    private void checkTimeout() {
        Long currTime = new Date().getTime();
        if (currTime - startedAt > Constants.TIMEOUT_START_MS && compState < STATE_DISPLAYED) {
            log.warn("Component [{}] start timeout", compName);
            compState = STATE_TIMEOUT;
        }
    }

    private void checkByDumpSys() {
        String dumpSysType = switch (this.compType) {
            case CompModel.TYPE_ACTIVITY -> "activity";
            case CompModel.TYPE_SERVICE -> "service";
            default -> null;
        };
        if (dumpSysType == null) {
            log.debug("Unsupported component type: " + this.compType);
            return;
        }
        String res = adb.shellSync("dumpsys", dumpSysType);
        if (res == null) {
            log.warn("Call dumpsys failed");
            return;
        }
        Pattern pattern = Pattern.compile("mFocusedActivity: (?<type>[^{]+)\\{(?<id>\\S+)\\s" +
                "(?<user>\\S+)\\s(?<compName>\\S+)\\s(?<tName>[^}]+)");
        Matcher matcher = pattern.matcher(res);
        if (!matcher.find() || !(matcher.groupCount() == 5)) {
            log.debug("No mFocusedActivity detected in dumpsys result");
            return;
        }
        String currCompName = matcher.group("compName");
        focusedActivity = currCompName;
        if (compName.equals(currCompName)) {
            long nowTime = new Date().getTime();
            if (displayedAt != null) {
                if (nowTime - displayedAt >= Constants.TIME_DISPLAY_REQUIRE_MS) {
                    compState = STATE_SUCCESS;
                }
            } else {
                displayedAt = nowTime;
            }
        } else if (currCompName.contains(GlobalConfig.getAndroidLauncherPkgName())) {
            // JUMPED to launcher, identify as crash
            compState = STATE_APP_CRASHED;
        } else if (!currCompName.equals(Constants.CLIENT_COMP_NAME)) {
            // mFocusedActivity is neither component nor test client.
            // It may indicate that the component is redirected to another one.
            compState = STATE_JUMPED;
        }
    }

    public void setComponent(String pkgName, String compName, String compType) {
        this.pkgName = pkgName;
        if (!compName.contains("/")) {
            if (!compName.contains(pkgName)) {
                compName = pkgName + "/" + compName;
            } else {
                compName = pkgName + compName.replace(pkgName, "/");
            }
        }
        this.compName = compName;
        this.compType = compType;
    }

    public void onActivityDisplayed(String logCompName) {
        if (!logCompName.equals(this.compName)) return;
        if (compState != STATE_UNK) {
            log.warn("Component state not UNK, ignored");
            return;
        }
        compState = STATE_DISPLAYED;
        displayedAt = new Date().getTime();
        log.info("Activity displayed! Waiting for TIME_DISPLAY_REQUIRE");
    }

    public void onBeginOfCrash() {
        log.warn("Beginning of crash detected!!");
        compState = STATE_APP_CRASHED;
    }
}
