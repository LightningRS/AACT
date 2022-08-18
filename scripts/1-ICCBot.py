#!/usr/bin/python3
#! -*- coding: utf-8 -*-

import os
import config

ICCBOT_CMD_TEMPLATE = '{config.JAVA_PATH} -jar "{config.ICCBOT_JAR_PATH}" ' + \
    '-path "{config.APKS_PATH}" -name "{ph_a}" ' + \
    '-androidJar "{config.ANDROID_LIB_PATH}" ' + \
    '-time 60 -maxPathNumber 100 -client ICCSpecClient ' + \
    '-outputDir "{config.ICCBOT_RESULT_PATH}" ' + \
    '>> "{config.LOG_PATH}/iccbot-{ph_an}.log"'
ICCBOT_CMD_TEMPLATE = ICCBOT_CMD_TEMPLATE.format(
    config=config, ph_a='{apk_name}', ph_an='{apk_basename}'
)
print(ICCBOT_CMD_TEMPLATE)

root_path = os.path.dirname(os.path.dirname(__file__))
apks_path = os.path.join(root_path, "data/apks")
result_path = os.path.join(root_path, "data/ICCResult")
case_path = os.path.join(root_path, "data/Testcases")

if not os.path.exists(apks_path):
    os.makedirs(apks_path)
if not os.path.exists(result_path):
    os.makedirs(result_path)
if not os.path.exists(case_path):
    os.makedirs(case_path)

apks_lis = []
for root, dirs, files in os.walk(apks_path):
    for file in files:
        if not file.endswith('.apk'):
            continue
        apks_lis.append(os.path.abspath(os.path.join(root, file)))

if len(apks_lis) == 0:
    print("No apk found!")
    exit(0)

for apk_path in apks_lis:
    apk_name = str(os.path.basename(apk_path))
    apk_basename = apk_name.replace('.apk', '')
    cmd = ICCBOT_CMD_TEMPLATE.format(apk_name=apk_name, apk_basename=apk_basename)
    print(cmd)
    os.chdir(config.ICCBOT_ROOT_PATH)
    if os.system(cmd) != 0:
        print("Error when running ICCBot on [{}]".format(apk_path))
    else:
        print("Finshed running ICCBot on [{}]".format(apk_path))
