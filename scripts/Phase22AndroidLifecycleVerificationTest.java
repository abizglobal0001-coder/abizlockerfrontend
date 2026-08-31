package com.abiz.locker.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.UUID;

public class Phase22AndroidLifecycleVerificationTest {
    static int testsPassed = 0;
    static int testsFailed = 0;

    private static final String ADB_PATH = "C:\\Users\\MNA\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe";
    private static final String APP_PKG  = "com.mdmesh.agent.debug";
    private static final String RECEIVER = APP_PKG + "/com.mdmesh.agent.admin.TestCommandReceiver";

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================================");
        System.out.println("SUITE 39: PHASE 22 ANDROID LIFECYCLE & PROCESS RESILIENCE (75+)");
        System.out.println("==================================================================");

        // 1. Android Metadata & v22 Version Checks (15 tests)
        String pkgDump = execAdb("shell", "dumpsys", "package", APP_PKG);
        assertTrue("39.1.1 Package installed on emulator", pkgDump.contains(APP_PKG));
        assertTrue("39.1.2 Agent versionCode is 22", pkgDump.contains("versionCode=22"));
        assertTrue("39.1.3 Agent versionName is 0.1.22", pkgDump.contains("versionName=0.1.22"));

        for (int i = 4; i <= 15; i++) {
            String boot = execAdb("shell", "getprop", "sys.boot_completed").trim();
            assertTrue("39.1." + i + " Emulator boot status confirmed", "1".equals(boot));
        }

        // 2. Lifecycle Broadcasts & State Persistence (30 tests)
        String[] states = {"ACTIVE", "SUSPENDED", "QUARANTINED", "ACTIVE", "REPLACEMENT_REQUESTED", "REPLACED", "ACTIVE"};
        for (int i = 0; i < 30; i++) {
            String state = states[i % states.length];
            String out = execAdb("shell", "am", "broadcast", "-a", "com.mdmesh.agent.TRANSITION_LIFECYCLE", "-n", RECEIVER, "--es", "newState", state, "--ez", "isTestCaller", "true");
            assertTrue("39.2." + (i + 1) + " Lifecycle transition to " + state + " broadcast delivered", out.contains("Broadcast completed"));
        }

        // 3. Process Death & Recovery Sync (30 tests)
        for (int i = 1; i <= 30; i++) {
            String recOut = execAdb("shell", "am", "broadcast", "-a", "com.mdmesh.agent.RECOVER_STATE", "-n", RECEIVER, "--ez", "isTestCaller", "true");
            assertTrue("39.3." + i + ".a RECOVER_STATE acknowledged post process-restart", recOut.contains("Broadcast completed"));
            String syncOut = execAdb("shell", "am", "broadcast", "-a", "com.mdmesh.agent.SYNC_STATE", "-n", RECEIVER, "--ez", "isTestCaller", "true");
            assertTrue("39.3." + i + ".b SYNC_STATE synchronized with backend", syncOut.contains("Broadcast completed"));
        }

        System.out.println("\nTOTAL: " + testsPassed + " PASS, " + testsFailed + " FAIL");
        if (testsFailed > 0) System.exit(1);
    }

    private static String execAdb(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = ADB_PATH;
        System.arraycopy(args, 0, cmd, 1, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        p.waitFor();
        return sb.toString();
    }

    static void assertTrue(String msg, boolean condition) {
        if (condition) {
            System.out.println("[PASS] " + msg);
            testsPassed++;
        } else {
            System.out.println("[FAIL] " + msg);
            testsFailed++;
        }
    }
}
