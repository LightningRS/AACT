#!/usr/bin/python3
#! -*- coding: utf-8 -*-

import os
import config

AACT_EXTRA = ''
if config.DEVICE_SERIAL is not None:
    AACT_EXTRA += ' -d "{config.DEVICE_SERIAL}"'.format(config=config)

AACT_CMD = '{config.JAVA_PATH} -jar "{config.AACT_JAR_PATH}" ' + \
    '-k "{config.APKS_PATH}" -i "{config.ICCBOT_RESULT_PATH}" ' + \
    '-c "{config.TESTCASE_PATH}" -st "{config.STRATEGIES}" ' + \
    '-ng -ce -a "{config.ADB_PATH}" -ln "{config.LAUNCHER_PKG_NAME}"'
AACT_CMD = AACT_CMD.format(config=config) + AACT_EXTRA
print(AACT_CMD)

if os.system(AACT_CMD) != 0:
    print("Failed to call AACT! cmd={}".format(AACT_CMD))
else:
    print("Finished calling AACT")
