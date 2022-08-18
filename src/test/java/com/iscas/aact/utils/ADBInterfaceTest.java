package com.iscas.aact.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ADBInterfaceTest {
    private static final String ADB_PATH = "D:/SDKs/AndroidSDK/platform-tools/adb.exe";
    private static final String DEVICE_SERIAL = "emulator-5556";

    @org.junit.jupiter.api.Test
    void testGetInstance() {
        ADBInterface adb = ADBInterface.getInstance();
        assertEquals(adb.getClass(), ADBInterface.class);
    }

    @org.junit.jupiter.api.Test
    void testSetAndGetADBExecPath() {
        ADBInterface adb = ADBInterface.getInstance();
        Path fakeADBPath = Paths.get("fakeADB").toAbsolutePath();
        try {
            Files.createFile(fakeADBPath);
            adb.setADBExecPath(fakeADBPath.toString());
            assertEquals(adb.getADBExecPath(), fakeADBPath);
            assertEquals(adb.getADBExecPathStr(), fakeADBPath.toString());
            Files.delete(fakeADBPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean connectIfNot() {
        ADBInterface adb = ADBInterface.getInstance();
        if (!adb.isConnected()) {
            adb.setADBExecPath(ADB_PATH);
            return adb.connect(DEVICE_SERIAL);
        }
        return true;
    }

    @org.junit.jupiter.api.Test
    void testConnect() {
        ADBInterface adb = ADBInterface.getInstance();
        adb.disconnect();
        assertFalse(adb.isConnected());
        assertTrue(this.connectIfNot());
    }

    @org.junit.jupiter.api.Test
    void testForward() {
        ADBInterface adb = ADBInterface.getInstance();
        assertTrue(this.connectIfNot());
        Integer localPort = adb.forward(1234);
        assertNotNull(localPort);
        Integer failPort = adb.forward(localPort, 1235);
        assertNull(failPort);
        assertTrue(adb.removeForward(localPort));
    }
}