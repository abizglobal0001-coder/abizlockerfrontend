package com.abiz.locker.test;

import com.abiz.locker.command.ProductionObservabilityService;
import java.util.*;

public class Phase22ObservabilityVerificationTest {
    static int testsPassed = 0;
    static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("SUITE 35: PHASE 22 PRODUCTION OBSERVABILITY VERIFICATION (75+)");
        System.out.println("==================================================================");

        ProductionObservabilityService obs = new ProductionObservabilityService();

        // 1. Metric Ingestion & Latency Tracking (30 tests)
        for (int i = 1; i <= 30; i++) {
            long latency = 10 + (i % 25);
            obs.recordCommandLatency("EXEC_LOCK", latency);
            obs.recordRequest("/api/v1/commands/create", latency + 5, true);
            assertTrue("35.1." + i + " Latency recorded for sample " + i, obs.getTotalCommands() >= i);
        }

        // 2. Percentile Calculations P50/P95/P99 (20 tests)
        for (int i = 1; i <= 20; i++) {
            Map<String, Double> p = obs.getLatencyPercentiles("EXEC_LOCK");
            assertNotNull("35.2." + i + ".a P50 non-null", p.get("P50"));
            assertNotNull("35.2." + i + ".b P95 non-null", p.get("P95"));
            assertNotNull("35.2." + i + ".c P99 non-null", p.get("P99"));
            assertTrue("35.2." + i + ".d P99 >= P50", p.get("P99") >= p.get("P50"));
        }

        // 3. Secret Redaction Invariant (25 tests)
        String[] sensitiveKeys = {"password", "secret", "token", "bearer", "privateKey", "credential"};
        for (int i = 0; i < 25; i++) {
            String key = sensitiveKeys[i % sensitiveKeys.length];
            String raw = "User action failed with " + key + "=super_secret_val_" + i + " at endpoint";
            String redacted = obs.redactSecret(raw);
            assertFalse("35.3." + (i + 1) + ".a Secret not present in plaintext", redacted.contains("super_secret_val_" + i));
            assertTrue("35.3." + (i + 1) + ".b Redacted marker applied", redacted.contains("***REDACTED***"));
        }

        System.out.println("\nTOTAL: " + testsPassed + " PASS, " + testsFailed + " FAIL");
        if (testsFailed > 0) System.exit(1);
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

    static void assertFalse(String msg, boolean condition) {
        assertTrue(msg, !condition);
    }

    static void assertNotNull(String msg, Object obj) {
        assertTrue(msg, obj != null);
    }
}
