#!/usr/bin/python3
#! -*- coding: utf-8 -*-

import os
import config

s_ia = input("Start apkIndex: ")
s_ic = input("Start compIndex: ")
s_it = input("Start caseIndex: ")
s_is = input("Start strategy: ")

AACT_EXTRA = ''
if config.SCOPE_CFG_PATH is not None:
    AACT_EXTRA += ' -o "{config.SCOPE_CFG_PATH}"'.format(config=config)
if config.RAND_SEED is not None:
    AACT_EXTRA += ' -s "{config.RAND_SEED}"'.format(config=config)
if config.STR_LEN_MIN is not None:
    AACT_EXTRA += ' -smin "{config.STR_LEN_MIN}"'.format(config=config)
if config.STR_LEN_MAX is not None:
    AACT_EXTRA += ' -smax "{config.STR_LEN_MAX}"'.format(config=config)
if config.DEVICE_SERIAL is not None:
    AACT_EXTRA += ' -d "{config.DEVICE_SERIAL}"'.format(config=config)

AACT_EXTRA += ' -ia {s_ia}'.format(s_ia=s_ia)
AACT_EXTRA += ' -ic {s_ic}'.format(s_ic=s_ic)
AACT_EXTRA += ' -it {s_it}'.format(s_it=s_it)
AACT_EXTRA += ' -is "{s_is}"'.format(s_is=s_is)

AACT_CMD = '{config.JAVA_PATH} -jar "{config.AACT_JAR_PATH}" ' + \
    '-k "{config.APKS_PATH}" -i "{config.ICCBOT_RESULT_PATH}" ' + \
    '-c "{config.TESTCASE_PATH}" -st "{config.STRATEGIES}" ' + \
    '-ag -ce -a "{config.ADB_PATH}" -ln "{config.LAUNCHER_PKG_NAME}"'
AACT_CMD = AACT_CMD.format(config=config) + AACT_EXTRA
print(AACT_CMD)

if os.system(AACT_CMD) != 0:
    print("Failed to call AACT! cmd={}".format(AACT_CMD))
else:
    print("Finished calling AACT")
