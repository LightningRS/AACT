package com.iscas.aact;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.iscas.aact.logcat.LogcatMonitor;
import com.iscas.aact.logcat.handler.StackTraceHandler;
import com.iscas.aact.logcat.utils.TraceBlock;
import com.iscas.aact.rpc.CompStateMonitor;
import com.iscas.aact.rpc.RPCController;
import com.iscas.aact.testcase.ACTSTestcaseBuilder;
import com.iscas.aact.testcase.ScopeConfigUtil;
import com.iscas.aact.testcase.TestcaseBuilder;
import com.iscas.aact.testcase.provider.*;
import com.iscas.aact.utils.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Getter
@Setter
@Slf4j
public class TestController {
    public static final int STATE_READY = 0;
    public static final int STATE_LOADING_TESTCASE = 1;
    public static final int STATE_LOADED_TESTCASE = 2;
    public static final int STATE_RUNNING_TESTCASE = 3;
    public static final int STATE_ERROR = 4;

    private static final Config GlobalConfig = Config.getInstance();
    private static final ADBInterface adb = ADBInterface.getInstance();
    private final LogcatMonitor logcatMonitor;
    private final CompStateMonitor compStateMonitor;
    private RPCController rpcController;

    private List<Path> apksPath;
    private ApkMeta currApkMeta;
    private CompModelUtil currAppModel;
    private CompModel currCompModel;
    private final ScopeConfigUtil scopeConfig;
    private Integer currCompState = STATE_READY;
    private Integer currCaseCount = 0;

    public TestController() {
        logcatMonitor = new LogcatMonitor(this);
        compStateMonitor = new CompStateMonitor(this);
        rpcController = null;
        scopeConfig = new ScopeConfigUtil();
        boolean hasPath = GlobalConfig.getScopeConfigPath() != null;
        scopeConfig.loadScopeConfig(
                hasPath ? GlobalConfig.getScopeConfigPath().toString() : Constants.DEFAULT_SCOPE_CONFIG, hasPath
        );
    }

    public void start() {
        // Load APKs
        this.loadApks();

        if (GlobalConfig.getTestGenMode() != TestGenMode.ONLY) {
            // Initialize ADB
            adb.setADBExecPath(GlobalConfig.getAdbPath().toString());
            if (!adb.connect(GlobalConfig.getDeviceSerial())) {
                return;
            }

            // Initialize logcat
            this.logcatMonitor.start();

            // Restart test bridge
            adb.forceStopApp(Constants.CLIENT_PKG_NAME);
            adb.startActivity(Constants.CLIENT_PKG_NAME, Constants.CLIENT_ACT_NAME);

            // Wait until RPCController ready
            while (rpcController == null || !rpcController.isReady()) {
                Thread.yield();
            }
        }
        this.process();
    }

    public void initRPC(Integer remotePort) {
        Integer localPort = adb.forward(remotePort);
        if (this.rpcController == null) {
            this.rpcController = new RPCController(this, localPort);
        } else {
            this.rpcController.setForwardPort(localPort);
        }
        this.rpcController.init();
    }

    public void process() {
        int currApkIndex = GlobalConfig.getStartApkIndex();
        int currCompIndex = GlobalConfig.getStartCompIndex();
        int currCaseIndex = GlobalConfig.getStartCaseIndex();
        while (currApkIndex < apksPath.size()) {
            int apkUniqueCrashesCnt = 0;
            log.info("Processing APK [{}] ({}/{})", apksPath.get(currApkIndex), currApkIndex + 1, apksPath.size());
            Path apkPath = apksPath.get(currApkIndex);
            if (!loadApk(apkPath)) {
                log.error("Failed to load APK #{}: {}, skipped", currApkIndex + 1, apkPath);
                continue;
            }

            // Check whether the APK is installed
            if (!adb.isPackageInstalled(currApkMeta.getPackageName())) {
                log.error("APK [{}] ({}) is not installed! Installing...", currApkMeta.getPackageName(), apkPath);
                if (!adb.installSync(apkPath.toString())) {
                    log.error("Failed to install apk [{}] ({})! Skip test", currApkMeta.getPackageName(), apkPath);

                    // Reset recovery parameters
                    GlobalConfig.setStartStrategy(null);
                    currCaseIndex = 0;
                    currCompIndex = 0;
                    currApkIndex++;

                    // Continue for next apk
                    continue;
                }
            }
            while (currCompIndex < currAppModel.getCompCount()) {
                // Stack trace body -> Failed Message
                Map<String, String> uniqueStackTraces = new HashMap<>();

                currCompModel = currAppModel.getCompModelByIndex(currCompIndex);
                JSONObject compJson = currAppModel.getCompModelJSONByIndex(currCompIndex);

                // -------------- Only support activity now --------------
                if (!currCompModel.getType().equals(CompModel.TYPE_ACTIVITY)) {
                    log.warn("Skipped unsupported component type: " + currCompModel.getType());
                    currCompIndex++;
                    continue;
                }

                log.info("Processing component [{}] ({}/{})",
                        currCompModel.getClassName(), currCompIndex + 1, currAppModel.getCompCount());

                // Detect current strategy settings
                Collection<String> readyStrategies = new HashSet<>();
                Collection<String> pendingStrategies = new HashSet<>();
                String valueStrategy = GlobalConfig.getStrategy();
                List<String> strategyGroups = Arrays.stream(valueStrategy.split(";"))
                        .distinct().filter(Predicate.not(String::isBlank)).map(String::trim).toList();
                for (String strategyGroup : strategyGroups) {
                    List<String> providerNames = Arrays.stream(strategyGroup.split("\\+"))
                            .distinct().filter(Predicate.not(String::isBlank)).sorted().map(String::trim).toList();
                    String strategyName = String.join("+", providerNames);

                    Path compTestcasePath = Paths.get(GlobalConfig.getTestcasePath().toString(),
                            currAppModel.getPackageName() + "/" + currCompModel.getClassName() + "_" +
                                    strategyName + ".csv");
                    if (Files.exists(compTestcasePath)) {
                        readyStrategies.add(strategyName);
                    } else {
                        pendingStrategies.add(strategyName);
                    }
                }

                if (!GlobalConfig.getTestGenMode().equals(TestGenMode.NONE)) {
                    // Only generate pending strategies
                    Map<String, ValueProvider> valueProviders = new HashMap<>() {
                        {
                            put("preset", new ValueProviderPreset());
                            put("iccBot", new ValueProviderICCBot(
                                    compJson.getJSONObject("fullValueSet"), scopeConfig
                            ));
                            put("random", new ValueProviderRandom(
                                    GlobalConfig.getRandValNum(),
                                    GlobalConfig.getStrMinLength(), GlobalConfig.getStrMaxLength()
                            ));
                            put("randomWithStruct", new ValueProviderRandomWithStruct(
                                    compJson.getJSONObject("fullValueSet"),
                                    scopeConfig, GlobalConfig.getRandValNum(),
                                    GlobalConfig.getStrMinLength(), GlobalConfig.getStrMaxLength()
                            ));
                        }
                    };
                    for (String strategyName : pendingStrategies) {
                        List<String> providerNames = Arrays.stream(strategyName.split("\\+"))
                                .map(String::trim).toList();
                        Path outputPath = Paths.get(
                                GlobalConfig.getTestcasePath().toString(), currAppModel.getPackageName(),
                                currCompModel.getClassName() + "_" + strategyName + ".csv");
                        Path outputDir = outputPath.getParent().toAbsolutePath();
                        if (!Files.exists(outputDir)) {
                            try {
                                Files.createDirectories(outputDir);
                            } catch (IOException e) {
                                log.error("Failed to create directory [" + outputDir + "]");
                                System.exit(1);
                            }
                        }
                        log.info("Generating by " + strategyName);
                        TestcaseBuilder builder = new ACTSTestcaseBuilder(compJson, scopeConfig);
                        for (String providerName : providerNames) {
                            builder.addValueProvider(valueProviders.get(providerName));
                        }
                        builder.collect();
                        builder.build(outputPath.toString());
                        if (Files.exists(outputPath)) {
                            readyStrategies.add(strategyName);
                            log.info("Finished generating by " + strategyName);
                        } else {
                            log.warn("No testcase generated by " + strategyName);
                        }
                    }
                    pendingStrategies.removeAll(readyStrategies);

                    if (GlobalConfig.getTestGenMode().equals(TestGenMode.ONLY)) {
                        currCompIndex++;
                        continue;
                    }
                }
                if (pendingStrategies.size() > 0) {
                    // Report error if there are still pending strategies
                    log.warn("No testcases under strategies [{}] for component [{}]",
                            String.join(", ", pendingStrategies), currCompModel.getClassName());
                }
                log.info("{} strategies [{}] are ready for testing component [{}]", readyStrategies.size(),
                        String.join(", ", readyStrategies), currCompModel.getClassName());

                // Resort ready strategies
                SortedArrayList<String> sortedReadyStrategies = new SortedArrayList<>();
                sortedReadyStrategies.addAll(readyStrategies);
                readyStrategies = sortedReadyStrategies;

                // Push all testcases
                for (String strategy : readyStrategies) {
                    Path localPath = Paths.get(
                            GlobalConfig.getTestcasePath().toString(), currAppModel.getPackageName(),
                            currCompModel.getClassName() + "_" + strategy + ".csv");
                    Path remotePath = Paths.get(
                            Constants.CLIENT_CASE_ROOT, currAppModel.getPackageName(),
                            currCompModel.getClassName() + "_" + strategy + ".csv");
                    log.debug("Pushing testcase [{}]", localPath.getFileName());
                    if (!adb.pushSync(localPath.toString(), remotePath.toString().replaceAll("\\\\", "/"))) {
                        log.error("Failed to push testcase [{}] to [{}]!", localPath, remotePath);
                        if (!GlobalConfig.getContinueIfError()) {
                            return;
                        }
                    }
                }
                log.info("Finished pushing all testcases to client");

                compStateMonitor.setComponent(currAppModel.getPackageName(), currCompModel.getClassName(),
                        currCompModel.getType());
                for (String strategy : readyStrategies) {
                    if (GlobalConfig.getStartStrategy() != null && !GlobalConfig.getStartStrategy().equals(strategy)) {
                        continue;
                    }
                    GlobalConfig.setStartStrategy(null);
                    log.info("Using strategy [{}]", strategy);
                    // Waiting for testcase load finished
                    setCurrCompState(STATE_LOADING_TESTCASE);
                    rpcController.loadTestcase(currApkMeta.getPackageName(), currCompModel.getClassName(),
                            currCompModel.getType(), strategy);
                    while (currCompState == STATE_LOADING_TESTCASE) {
                        Thread.yield();
                    }
                    if (currCompState != STATE_LOADED_TESTCASE) {
                        log.error("Unexpected state after loading case! state={}, comp={}, strategy={}",
                                currCompState, currCompModel.getClassName(), strategy);
                        if (GlobalConfig.getContinueIfError()) {
                            currCompIndex++;
                            continue;
                        } else {
                            return;
                        }
                    }

                    // Run testcases
                    while (currCaseIndex < currCaseCount) {
                        String recoveryInfo = String.format(
                                "compName=%s, apkIndex=%s, compIndex=%s, caseIndex=%s, strategy=%s",
                                currCompModel.getClassName(), currApkIndex, currCompIndex, currCaseIndex, strategy
                        );

                        adb.forceStopApp(currAppModel.getPackageName());
                        log.info("Start to run testcase #{}/{}", currCaseIndex + 1, currCaseCount);
                        // Waiting for testcase run finished
                        setCurrCompState(STATE_RUNNING_TESTCASE);
                        rpcController.runTestcase(currCaseIndex);
                        compStateMonitor.setCompState(CompStateMonitor.STATE_UNK);

                        compStateMonitor.setComponent(currAppModel.getPackageName(),
                                currCompModel.getClassName(), currCompModel.getType());
                        compStateMonitor.start();

                        while (compStateMonitor.getCompState() < CompStateMonitor.STATE_RESULT) {
                            Thread.yield();
                        }

                        // Waiting for consuming all trace blocks
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        StackTraceHandler stHandler = logcatMonitor.getHandlerByClass(StackTraceHandler.class);
                        TraceBlock mergedTraceBlock = new TraceBlock();
                        while (stHandler.hasTraceBlock()) {
                            TraceBlock tb = stHandler.getTraceBlock();
                            if (tb.body.contains(currAppModel.getPackageName()) ||
                                    tb.body.contains(Constants.CLIENT_PKG_NAME)) {
                                if (mergedTraceBlock.headInfo == null) {
                                    mergedTraceBlock.headInfo = tb.headInfo;
                                }
                                if (mergedTraceBlock.body != null) {
                                    mergedTraceBlock.body += "\n" + tb.body;
                                } else {
                                    mergedTraceBlock.body = tb.body;
                                }
                            }
                        }

                        if (compStateMonitor.getCompState() <= CompStateMonitor.STATE_CLIENT_ERROR) {
                            log.error("Case running ERROR! Try to rollback! state={}, mFocusedActivity={}, {}",
                                    compStateMonitor.getCompStateName(), compStateMonitor.getFocusedActivity(),
                                    recoveryInfo);
                            continue;
                        }

                        // Retrieve stack trace block
                        if (mergedTraceBlock.headInfo == null) {
                            if (compStateMonitor.getCompState() < CompStateMonitor.STATE_JUMPED) {
                                log.error("No stacktrace is caught!");
                            }
                        } else {
                            log.warn("Stacktrace caught: {}\n{}", mergedTraceBlock.headInfo, mergedTraceBlock.body);
                            if (mergedTraceBlock.body.contains("android.content.ActivityNotFoundException")) {
                                // If ActivityNotFoundException detected, it means the component is failed to find.
                                // Should skip the component directly.
                                log.error("ActivityNotFoundException occurred, skip the current component!");
                                break;
                            }
                        }

                        if (compStateMonitor.getCompState() < CompStateMonitor.STATE_JUMPED) {
                            log.error("Case FAILED! state={}, mFocusedActivity={}, {}",
                                    compStateMonitor.getCompStateName(), compStateMonitor.getFocusedActivity(),
                                    recoveryInfo);

                            if (compStateMonitor.getCompState() >= CompStateMonitor.STATE_APP_CRASHED) {
                                if (mergedTraceBlock.headInfo != null &&
                                        !uniqueStackTraces.containsKey(mergedTraceBlock.body)) {
                                    String resultMsg = String.format(
                                            "state=%s, %s", compStateMonitor.getCompStateName(), recoveryInfo
                                    );
                                    uniqueStackTraces.put(mergedTraceBlock.body, resultMsg);
                                    log.info("Unique crash #{} detected! " + resultMsg, uniqueStackTraces.size());
                                }
                                if (!GlobalConfig.getContinueIfError()) return;
                            }
                        } else if (compStateMonitor.getCompState().equals(CompStateMonitor.STATE_JUMPED)) {
                            log.info("Case JUMPED! mFocusedActivity={}, {}",
                                    compStateMonitor.getFocusedActivity(), recoveryInfo);
                        } else {
                            log.info("Case PASSED! state={}, mFocusedActivity={}, {}",
                                    compStateMonitor.getCompStateName(), compStateMonitor.getFocusedActivity(),
                                    recoveryInfo);
                        }
                        // Testcase finished
                        log.info("Finished running testcase #{}", currCaseIndex + 1);
                        currCaseIndex++;
                    }
                    currCaseIndex = 0;
                }
                adb.forceStopApp(currAppModel.getPackageName());
                log.info("Finished testing component [{}], with {} unique crashes",
                        currCompModel.getClassName(), uniqueStackTraces.size());
                apkUniqueCrashesCnt += uniqueStackTraces.size();
                currCompIndex++;
            }
            if (!GlobalConfig.getTestGenMode().equals(TestGenMode.ONLY)) {
                adb.forceStopApp(currAppModel.getPackageName());
                log.info("Finished testing APK [{}], with {} unique crashes", apksPath.get(currApkIndex), apkUniqueCrashesCnt);
            } else {
                log.info("Finished generating testcases for APK [{}]", apksPath.get(currApkIndex));
            }
            currCompIndex = 0;
            currApkIndex++;
        }
    }

    private void loadApks() {
        apksPath = new ArrayList<>();
        if (!GlobalConfig.isApkPathDirectory()) {
            apksPath.add(GlobalConfig.getApkPath());
        } else {
            try (Stream<Path> entries = Files.walk(GlobalConfig.getApkPath())) {
                entries.forEach(path -> {
                    if (path.toString().endsWith(".apk")) apksPath.add(path);
                });
            } catch (IOException e) {
                log.error("IOException when walking apk directory: {}", GlobalConfig.getApkPath());
            }
        }
        log.info("Found totally {} apks to run", apksPath.size());
    }

    private boolean loadApk(Path apkPath) {
        try (ApkFile apkFile = new ApkFile(apkPath.toFile())) {
            currApkMeta = apkFile.getApkMeta();
            String apkFileName = apkPath.getFileName().toString();
            String apkFileBaseName = apkFileName.substring(0, apkFileName.lastIndexOf("."));

            // Load Component Model extracted by ICCBot
            Path compModelJsonPath = Paths.get(
                    GlobalConfig.getIccResultPath().toString(),
                    apkFileBaseName + "/ICCSpecification/ComponentModel.json");
            if (!Files.exists(compModelJsonPath)) {
                log.error("ComponentModel file [{}] for app [{}] ({}) not found!",
                        compModelJsonPath, apkFileBaseName, currApkMeta.getPackageName());
                return false;
            }
            String compModelStr = Files.readString(compModelJsonPath);
            JSONObject compModel = JSON.parseObject(compModelStr);
            currAppModel = new CompModelUtil(compModel);
            log.info("Loaded APK [{}], with {} components to be test", apkFileName, currAppModel.getCompCount());
            return true;
        } catch (IOException e) {
            log.error("IOException when loading apk: {}", apkPath, e);
            return false;
        } catch (JSONException e) {
            log.error("JSONException when loading apk: {}", apkPath, e);
            return false;
        }
    }

    public void setCurrCompState(int state) {
        if (state < STATE_READY || state > STATE_ERROR) {
            throw new IllegalArgumentException("Invalid component state: " + state);
        }
        this.currCompState = state;
    }

    public void setCurrCaseCount(int caseCount) {
        currCaseCount = caseCount;
    }
}
