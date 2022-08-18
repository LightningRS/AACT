package com.iscas.aact.utils;

import com.iscas.aact.Constants;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class ArgParser {
    private static final Logger Log = LoggerFactory.getLogger(ArgParser.class);
    private static final Options options = getOptions();

    public static boolean parse(String[] args) {
        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            Log.error("Invalid options: {}", e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            System.out.print("\n");
            formatter.printHelp(
                    120,
                    "java -jar TestController.jar",
                    "\nAndroid Application Component Test Controller",
                    options,
                    "\nFor more information, please refers to https://tool-introduction-page\n\n"
            );
            return false;
        }
        Config config = Config.getInstance();

        // ------------------------ Non-null parameters ------------------------

        Path apkPath = Paths.get(cmd.getOptionValue("k")).toAbsolutePath();
        if (!Files.isReadable(apkPath)) {
            Log.error("APK path is not readable: {}", apkPath);
            return false;
        }
        config.setApkPath(apkPath);
        config.setApkPathDirectory(Files.isDirectory(apkPath));

        Path iccResultPath = Paths.get(cmd.getOptionValue("i")).toAbsolutePath();
        if (!Files.isDirectory(iccResultPath) || !Files.isReadable(iccResultPath)) {
            Log.error("Invalid ICCBot result path: {}", iccResultPath);
            return false;
        }
        config.setIccResultPath(iccResultPath);

        Path testcasePath = Paths.get(cmd.getOptionValue("c")).toAbsolutePath();
        if (!Files.isDirectory(testcasePath) || !Files.isReadable(testcasePath)) {
            Log.error("Invalid testcase directory: {}", testcasePath);
            return false;
        }
        config.setTestcasePath(testcasePath);

        config.setStrategy(cmd.getOptionValue("st"));

        // ------------------------ Nullable parameters ------------------------

        if (cmd.hasOption("o")) {
            Path scopeConfigPath = Paths.get(cmd.getOptionValue("o")).toAbsolutePath();
            if (!Files.isRegularFile(scopeConfigPath) || !Files.isReadable(scopeConfigPath)) {
                Log.error("Invalid scope config file: {}", scopeConfigPath);
                return false;
            }
            config.setScopeConfigPath(scopeConfigPath);
        }

        config.setContinueIfError(cmd.hasOption("ce"));
        config.setSeed(cmd.hasOption("s") ? Long.parseLong(cmd.getOptionValue("s")) : new Date().getTime());
        config.setDeviceSerial(cmd.getOptionValue("d"));
        config.setStartApkIndex(cmd.hasOption("ia") ? Integer.parseInt(cmd.getOptionValue("ia")) : 0);
        config.setStartCompIndex(cmd.hasOption("ic") ? Integer.parseInt(cmd.getOptionValue("ic")) : 0);
        config.setStartCaseIndex(cmd.hasOption("it") ? Integer.parseInt(cmd.getOptionValue("it")) : 0);
        config.setStartStrategy(cmd.getOptionValue("is"));

        config.setRandValNum(cmd.hasOption("r") ?
                Integer.parseInt(cmd.getOptionValue("r")) :
                Constants.DEFAULT_RAND_VAL_NUM);
        config.setStrMinLength(cmd.hasOption("smin") ?
                Integer.parseInt(cmd.getOptionValue("smin")) :
                Constants.DEFAULT_RAND_STR_MIN_LENGTH);
        config.setStrMaxLength(cmd.hasOption("smax") ?
                Integer.parseInt(cmd.getOptionValue("smax")) :
                Constants.DEFAULT_RAND_STR_MAX_LENGTH);

        if (cmd.hasOption("ag")) config.setTestGenMode(TestGenMode.AUTO);
        else if (cmd.hasOption("og")) config.setTestGenMode(TestGenMode.ONLY);
        else config.setTestGenMode(TestGenMode.NONE);

        if (cmd.hasOption("og")) {
            config.setTestGenMode(TestGenMode.ONLY);
        } else {
            // To run test, check parameters
            if (!cmd.hasOption("a")) {
                Log.error("ADB executable path cannot be empty");
                return false;
            }
            Path adbPath = Paths.get(cmd.getOptionValue("a")).toAbsolutePath();
            if (!Files.isExecutable(adbPath)) {
                Log.error("ADB is not executable: {}", adbPath);
                return false;
            }
            config.setAdbPath(adbPath);

            if (!cmd.hasOption("ln")) {
                Log.error("Android launcher package name cannot be empty");
                return false;
            }
            config.setAndroidLauncherPkgName(cmd.getOptionValue("ln"));

            if (cmd.hasOption("ag")) config.setTestGenMode(TestGenMode.AUTO);
            else config.setTestGenMode(TestGenMode.NONE);
        }
        return true;
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addRequiredOption("k", "apk-path", true,
                "[Required] Path to APK or a directory contains APK(s)");
        options.addRequiredOption("i", "icc-result-path", true,
                "[Required] Path to ICCBot result directory");
        options.addRequiredOption("c", "testcase-path", true,
                "[Required] Path to testcase directory");
        options.addRequiredOption("st", "strategy", true,
                "[Required] Strategy used for value provider. Supported strategies include preset, iccBot, random, " +
                        "and randomWithStruct. Use '+' as combination delimiter and ';' as group delimiter. Example: " +
                        "\"iccBot; preset+iccBot+random\"");

        options.addOption("a", "adb-path", true,
                "Path to ADB executable");
        options.addOption("ln", "android-launcher-package-name", true,
                "Android default launcher package name. Example: com.android.launcher3");
        options.addOption("o", "scope-config", true,
                "Path to scope config file. Default=null");
        options.addOption("d", "device", true,
                "Serial number of Android device. Default=null");
        options.addOption("s", "seed", true,
                "Random seed (long). Example: 12345678");
        options.addOption("r", "rand-val-num", true,
                "Number of the random values to generate for each field. Default=5");
        options.addOption("ce", "continue-if-error", false,
                "Continue if error occurred when testing components. If not specified, default is false");

        options.addOption("smin", "str-min-length", true,
                "Min length of the random string. Default=" + Constants.DEFAULT_RAND_STR_MIN_LENGTH);
        options.addOption("smax", "str-max-length", true,
                "Max length of the random string. Default=" + Constants.DEFAULT_RAND_STR_MAX_LENGTH);

        options.addOption("ia", "start-apk-index", true,
                "The start index of apk. Default=0");
        options.addOption("ic", "start-component-index", true,
                "The start index of component. Default=0");
        options.addOption("it", "start-testcase-index", true,
                "The start index of testcase. Default=0");
        options.addOption("is", "start-strategy", true,
                "The name of start strategy. Default is null");

        OptionGroup genGroup = new OptionGroup();
        Option autoGenOpt = new Option("ag", "auto-generate", false,
                "Generate testcases for component automatically if not exists");
        Option onlyGenOpt = new Option("og", "only-generate", false,
                "Only generate testcases (without running)");
        Option notGenOpt = new Option("ng", "not-generate", false,
                "Do not generate testcases if not exists");
        genGroup.addOption(autoGenOpt);
        genGroup.addOption(onlyGenOpt);
        genGroup.addOption(notGenOpt);
        genGroup.setRequired(true);
        options.addOptionGroup(genGroup);
        return options;
    }
}
