#!/usr/bin/python3
#! -*- coding: utf-8 -*-

import os

### Java Path
JAVA_PATH = "D:/Programs/jdk-18.0.1/bin/java.exe"

''' Uncomment for custom values
### Environment
ADB_PATH = "/path/to/adb-executable"
ICCBOT_ROOT_PATH = "/path/tp/iccbot-root"
'''

### AACT Properties START
STRATEGIES = 'iccBot; random; randomWithStruct; iccBot+preset; iccBot+preset+randomWithStruct'
RAND_VAL_NUM = 3
RAND_SEED = 12345678
DEVICE_SERIAL = 'emulator-5556'
LAUNCHER_PKG_NAME = 'com.android.launcher3'
SCOPE_CFG_PATH = None
STR_LEN_MIN = None
STR_LEN_MAX = None
### AACT Properties END

ROOT_PATH = os.path.abspath(os.path.dirname(os.path.dirname(__file__)))

''' Comment for custom values '''
### Environment
ADB_PATH = os.path.join(ROOT_PATH, "adb.exe")
ICCBOT_ROOT_PATH = os.path.join(ROOT_PATH, "ICCBot")
''' Comment for custom values '''

AACT_JAR_PATH = os.path.join(ROOT_PATH, "AACT-1.0.jar")
ICCBOT_JAR_PATH = os.path.join(ICCBOT_ROOT_PATH, "ICCBot.jar")
ANDROID_LIB_PATH = os.path.join(ICCBOT_ROOT_PATH, "lib/platforms")

LOG_PATH = os.path.join(ROOT_PATH, "logs")
DATA_PATH = os.path.join(ROOT_PATH, "data")
APKS_PATH = os.path.join(DATA_PATH, "APKs")
ICCBOT_RESULT_PATH = os.path.join(DATA_PATH, "ICCResult")
TESTCASE_PATH = os.path.join(DATA_PATH, "Testcases")

if not os.path.exists(JAVA_PATH):
    print("Java.exe not found!")
    exit(0)

if not os.path.exists(ADB_PATH):
    print("adb.exe not found!")
    exit(0)

if not os.path.exists(AACT_JAR_PATH):
    print("AACT.jar not found!")
    exit(0)

if not os.path.exists(ICCBOT_JAR_PATH):
    print("ICCBot.jar not found!")
    exit(0)

if not os.path.exists(ANDROID_LIB_PATH):
    print("Android library (platforms) not found!")
    exit(0)

os.makedirs(LOG_PATH, exist_ok=True)
os.makedirs(APKS_PATH, exist_ok=True)
os.makedirs(ICCBOT_RESULT_PATH, exist_ok=True)
os.makedirs(TESTCASE_PATH, exist_ok=True)
