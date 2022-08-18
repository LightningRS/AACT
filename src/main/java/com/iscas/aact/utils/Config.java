package com.iscas.aact.utils;

import lombok.Data;

import java.nio.file.Path;

@Data
public class Config {
    private static class ConfigHolder {
        private static final Config INSTANCE = new Config();
    }

    private Path adbPath;
    private String androidLauncherPkgName;
    private Path apkPath;
    private boolean isApkPathDirectory;
    private Path iccResultPath;
    private Path testcasePath;
    private Path scopeConfigPath;
    private String deviceSerial;
    private Long seed;
    private TestGenMode testGenMode;
    private int randValNum;
    private int strMinLength;
    private int strMaxLength;

    private String strategy;
    private Boolean continueIfError;

    private Integer startApkIndex;
    private Integer startCompIndex;
    private Integer startCaseIndex;
    private String startStrategy;

    private Config() {

    }

    public static Config getInstance() {
        return ConfigHolder.INSTANCE;
    }
}
