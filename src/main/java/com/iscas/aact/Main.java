package com.iscas.aact;

import com.iscas.aact.utils.ArgParser;

public class Main {
    public static void main(String[] args) {
        String[] fakeArgs = {
                "-a", "D:\\SDKs\\AndroidSDK\\platform-tools\\adb.exe",
                "-ln", "com.android.launcher3",
                "-k", "D:\\Projects\\android\\IntentFuzzing\\Test05APKs",
                "-i", "D:\\Projects\\android\\IntentFuzzing\\Test05ICCResults",
                "-c", "D:\\Projects\\android\\IntentFuzzing\\Test05Testcases",
                "-s", "12345678",
                "-r", "3",
                "-d", "emulator-5556",
                "-ag",
                "-ce",
                // "-oe",
                // "-ia", "4", "-ic", "2", "-it", "20", "-is", "iccBot",
                "-st", "iccBot; random; randomWithStruct; iccBot+preset; iccBot+preset+randomWithStruct"
        };
        /* if (!ArgParser.parse(fakeArgs)) {
            return;
        } */

        if (!ArgParser.parse(args)) {
            return;
        }
        TestController controller = new TestController();
        controller.start();
    }
}