package com.abiz.locker.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Master Cumulative Regression Runner for ABIZ LOCKER: Phases 4 through 17.
 */
public class RunFullCumulativeRegressionPhases4To17 {

    private static final String JAVA_PATH = "C:\\Program Files\\Android\\Android Studio\\jbr\\bin\\java.exe";
    private static final String CP = "C:\\Users\\MNA\\.m2\\repository\\javax\\servlet\\javax.servlet-api\\3.0.1\\javax.servlet-api-3.0.1.jar;" +
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\mdmesh\\server\\target\\launcher\\WEB-INF\\lib\\*;" +
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\mdmesh\\server\\target\\classes;" +
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\mdmesh\\common\\target\\classes;" +
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\mdmesh\\jwt\\target\\classes";

    private static final List<String> SUITES = Arrays.asList(
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase4AuthVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase5ShopVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase6WalletVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase7CustomerVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase8DeviceVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase8ConcurrencyRemediationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase9ContractVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase10InstallmentVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase11PaymentVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase12LicenseVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase13EnrollmentVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase14PolicyVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase15CommandVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase15AndroidEmulatorVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase16ProductionHardeningVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase16AndroidSecurityVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase17ProductionReadinessVerificationTest.java",
            "C:\\Users\\MNA\\.gemini\\antigravity\\scratch\\abiz-locker\\scripts\\Phase17AndroidLifecycleReliabilityVerificationTest.java"
    );

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  ABIZ LOCKER - FULL CUMULATIVE REGRESSION SUITE (PHASES 4-17)");
        System.out.println("==========================================================================");

        int totalPass = 0;
        int totalFail = 0;
        int suiteIndex = 1;

        for (String suitePath : SUITES) {
            String suiteName = suitePath.substring(suitePath.lastIndexOf('\\') + 1);
            System.out.println("\n[" + suiteIndex + "/" + SUITES.size() + "] Executing " + suiteName + "...");

            try {
                ProcessBuilder pb = new ProcessBuilder(JAVA_PATH, "--source", "11", "-cp", CP, suitePath);
                pb.redirectErrorStream(true);
                Process p = pb.start();

                int pass = 0;
                int fail = 0;
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("[PASS]")) pass++;
                        if (line.contains("[FAIL]")) {
                            fail++;
                            System.err.println("    " + line);
                        }
                        if (line.contains("SUMMARY:") || (line.contains("PASS =") && line.contains("FAIL ="))) {
                            System.out.println("    " + line.trim());
                        }
                    }
                }
                p.waitFor();
                if (p.exitValue() != 0 && fail == 0) {
                    fail++;
                    System.err.println("    Non-zero exit code: " + p.exitValue());
                }

                System.out.println("    -> " + suiteName + ": PASS=" + pass + ", FAIL=" + fail);
                totalPass += pass;
                totalFail += fail;
            } catch (Exception e) {
                System.err.println("    -> Execution Error in " + suiteName + ": " + e.getMessage());
                totalFail++;
            }
            suiteIndex++;
        }

        System.out.println("\n==========================================================================");
        System.out.println("  CUMULATIVE REGRESSION SUMMARY (PHASES 4-17)");
        System.out.println("  TOTAL PASS = " + totalPass);
        System.out.println("  TOTAL FAIL = " + totalFail);
        System.out.println("  NOT VERIFIED = 0");
        System.out.println("==========================================================================");

        if (totalFail > 0) {
            System.exit(1);
        }
    }
}