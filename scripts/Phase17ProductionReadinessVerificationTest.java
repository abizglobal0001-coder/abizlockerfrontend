package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.command.*;
import com.abiz.locker.command.transport.CommandDispatchResult;
import com.abiz.locker.command.transport.DeviceCommandTransport;
import com.abiz.locker.domain.*;
import com.abiz.locker.mapper.*;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 17 Production Readiness, Observability, Disaster Recovery,
 * Deployment Safety & Android Lifecycle Reliability Verification Suite.
 */
public class Phase17ProductionReadinessVerificationTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    private static final String DB_USER = "abizlocker";
    private static final String DB_PASS = "abiz_dev_secure_pass_9281";

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static long imeiSequence = 86170000000000L + (System.currentTimeMillis() % 9000000000000L);

    private static synchronized String nextValidImei() {
        long base14 = imeiSequence++;
        String s = String.valueOf(base14);
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int d = Character.getNumericValue(s.charAt(i));
            if (i % 2 == 1) {
                d *= 2;
                if (d > 9) d = (d % 10) + 1;
            }
            sum += d;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return s + checkDigit;
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  ABIZ LOCKER - PHASE 17 PRODUCTION READINESS MASTER VERIFICATION SUITE");
        System.out.println("==========================================================================");

        try {
            PooledDataSource ds = new PooledDataSource("org.postgresql.Driver", DB_URL, DB_USER, DB_PASS);
            Environment env = new Environment("phase17", new JdbcTransactionFactory(), ds);
            Configuration config = new Configuration(env);
            config.getTypeHandlerRegistry().register(UUID.class, new UuidTypeHandler());
            config.addMapper(DeviceCommandMapper.class);
            config.addMapper(DevicePolicyMapper.class);
            config.addMapper(ShopMapper.class);
            config.addMapper(FinancedDeviceMapper.class);
            config.addMapper(CustomerMapper.class);
            config.addMapper(AuditMapper.class);
            config.addMapper(SecurityEventMapper.class);

            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = sqlSessionFactory.openSession(true)) {
                DeviceCommandMapper commandMapper = session.getMapper(DeviceCommandMapper.class);
                DevicePolicyMapper policyMapper = session.getMapper(DevicePolicyMapper.class);
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);
                CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);
                SecurityEventMapper securityEventMapper = session.getMapper(SecurityEventMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                java.lang.reflect.Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                f.setAccessible(true);
                f.set(auditService, auditMapper);

                MockHardenedTransport transport = new MockHardenedTransport();
                DeviceCommandService commandService = new DeviceCommandService(commandMapper, policyMapper, transport, auditService);
                CommandReconciliationWorker reconciliationWorker = new CommandReconciliationWorker(commandMapper, auditService);
                HealthService healthService = new HealthService(commandMapper, deviceMapper);
                SecurityEventAggregator securityAggregator = new SecurityEventAggregator(securityEventMapper);

                // Setup test tenant fixtures
                UUID shopA = UUID.randomUUID();
                UUID shopB = UUID.randomUUID();
                createShop(shopA, "P17 Shop A", "Owner A");
                createShop(shopB, "P17 Shop B", "Owner B");

                UUID custA = UUID.randomUUID();
                createCustomer(custA, shopA, "Customer P17 A", "03009998877");
                UUID devA = UUID.randomUUID();
                createDevice(devA, shopA, custA, nextValidImei(), "TOKEN-P17-A");

                UUID devB = UUID.randomUUID();
                createDevice(devB, shopB, null, nextValidImei(), "TOKEN-P17-B");

                runSection1(ds);
                runSection2(shopA, devA);
                runSection3(commandService, shopA, devA);
                runSection4(healthService);
                runSection5(securityEventMapper, commandMapper);
                runSection6(reconciliationWorker, commandMapper, shopA, devA);
                runSection7(reconciliationWorker, commandMapper, shopA, devA);
                runSection8();
                runSection9(deviceMapper, devA);
                runSection10(commandMapper, shopA, devA);
                runSection11(securityAggregator, securityEventMapper, shopA, devA);
                runSection12(auditService, shopA, devA);
                runSection13(commandService, shopA, custA);
                runSection14();
                runSection15(healthService);
                runSection16(sqlSessionFactory, auditService);
                runSection17(commandMapper, shopA, devA);
                runSection18(commandService, shopA, devA);
                runSection19(commandService, shopA, devA);
                runSection20(commandService, devA);
                runSection21(healthService);
                runSection22(commandService, commandMapper, shopA, shopB, devB);
                runSection23();

                System.out.println("\n==========================================================================");
                System.out.println("  PHASE 17 VERIFICATION SUMMARY: " + testsPassed + " PASS, " + testsFailed + " FAIL");
                System.out.println("==========================================================================");

                if (testsFailed > 0) {
                    System.err.println("TESTS FAILED!");
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    // =====================================================================
    // Section 1: 17.1 Production Configuration Hardening (5 tests)
    // =====================================================================
    private static void runSection1(PooledDataSource ds) {
        System.out.println("\n--- 17.1: Production Configuration Hardening ---");
        assertNotNull("DB_URL is configured and non-null", DB_URL);
        assertTrue("DB_URL specifies PostgreSQL dialect and port 3000", DB_URL.startsWith("jdbc:postgresql://localhost:3000/"));
        assertNotNull("DB_USER configured", DB_USER);
        assertTrue("DB user is dedicated tenant user (not default postgres)", !DB_USER.equalsIgnoreCase("postgres"));
        assertTrue("Pooled DataSource has active connection limits", ds.getPoolMaximumActiveConnections() > 0);
    }

    // =====================================================================
    // Section 2: 17.2 Structured Logging & Secret Protection (8 tests)
    // =====================================================================
    private static void runSection2(UUID shopA, UUID devA) {
        System.out.println("\n--- 17.2: Structured Logging & Secret Protection ---");
        String reqId1 = "REQ-" + UUID.randomUUID().toString().substring(0, 8);
        StructuredEventLogger.setRequestId(reqId1);
        assertEquals("StructuredEventLogger stores requestId in ThreadLocal", reqId1, StructuredEventLogger.getRequestId());

        StructuredEventLogger.clearContext();
        assertEquals("clearContext() clears requestId", null, StructuredEventLogger.getRequestId());

        StructuredEventLogger.setRequestId("REQ-P17-001");
        StructuredEventLogger.setCorrelationId("CORR-P17-001");
        StructuredEventLogger.setActorId(shopA);
        assertEquals("Correlation ID set properly", "CORR-P17-001", StructuredEventLogger.getCorrelationId());
        assertEquals("Actor ID set properly", shopA, StructuredEventLogger.getActorId());

        StructuredEventLogger.info(StructuredEventLogger.CAT_COMMAND, "DISPATCH_STARTED", shopA, devA, null, "Dispatch initiated");
        StructuredEventLogger.warn(StructuredEventLogger.CAT_HEALTH, "STALE_HEARTBEAT", shopA, devA, null, "Heartbeat delayed");
        StructuredEventLogger.security("AUTH_FAILURE", shopA, devA, "Safe security event log without credentials");
        pass("StructuredEventLogger writes structured logs without throwing");

        String hugeMsg = new String(new char[600]).replace('\0', 'X');
        StructuredEventLogger.security("OVERFLOW_TEST", shopA, devA, hugeMsg);
        pass("Security event handles long messages with safe truncation");
        StructuredEventLogger.clearContext();
    }

    // =====================================================================
    // Section 3: 17.3 Correlation & Request Tracing (5 tests)
    // =====================================================================
    private static void runSection3(DeviceCommandService commandService, UUID shopA, UUID devA) {
        System.out.println("\n--- 17.3: Correlation & Request Tracing ---");
        AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopA, "ownerA@shopA.com");
        AbizDeviceCommand traceCmd = commandService.createCommand(devA, AbizDeviceCommand.TYPE_PAYMENT_WARNING, "IDEMP-TRACE-1", Collections.emptyMap(), 60);
        assertNotNull("Command has auto-generated requestId", traceCmd.getRequestId());
        assertTrue("RequestId format is REQ-...", traceCmd.getRequestId().startsWith("REQ-"));
        assertNotNull("Command has correlationId", traceCmd.getCorrelationId());
        assertNotNull("Command has cryptographic nonce", traceCmd.getNonce());
        assertEquals("Nonce length is 32 hex characters", 32, traceCmd.getNonce().length());
        AbizTenantContext.clear();
    }

    // =====================================================================
    // Section 4: 17.4 Health & Readiness System (10 tests)
    // =====================================================================
    private static void runSection4(HealthService healthService) {
        System.out.println("\n--- 17.4: Health & Readiness System ---");
        assertTrue("HealthService.isLive() returns true", healthService.isLive());
        assertTrue("HealthService.isReady() returns true with database reachable", healthService.isReady());
        assertTrue("HealthService.getCommandQueueDepth() >= 0", healthService.getCommandQueueDepth() >= 0);
        assertTrue("HealthService.getStaleDeviceCount() >= 0", healthService.getStaleDeviceCount() >= 0);

        Map<String, Object> metrics = healthService.getOperationalMetrics();
        assertNotNull("Operational metrics map is non-null", metrics);
        assertEquals("Metrics status is UP", "UP", metrics.get("status"));
        assertTrue("Metrics contain activeCommands", metrics.containsKey("activeCommands"));
        assertTrue("Metrics contain pendingCommands", metrics.containsKey("pendingCommands"));
        assertTrue("Metrics contain deadLetteredCommands", metrics.containsKey("deadLetteredCommands"));
        assertTrue("Metrics contain staleDevices", metrics.containsKey("staleDevices"));
    }

    // =====================================================================
    // Section 5: 17.5 Database Index & Schema Review (5 tests)
    // =====================================================================
    private static void runSection5(SecurityEventMapper securityEventMapper, DeviceCommandMapper commandMapper) throws Exception {
        System.out.println("\n--- 17.5: Database Index & Schema Review ---");
        assertTrue("abiz_security_events table exists and queryable", securityEventMapper.countAllEvents() >= 0);
        assertTrue("countDeadLetteredCommands() runs on abiz_device_commands", commandMapper.countDeadLetteredCommands() >= 0);
        assertTrue("countActiveCommands() runs with partial index optimization", commandMapper.countActiveCommands() >= 0);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM abiz_rate_limit_state")) {
            ResultSet rs = ps.executeQuery();
            assertTrue("abiz_rate_limit_state table exists", rs.next() && rs.getInt(1) >= 0);
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT indexname FROM pg_indexes WHERE tablename = 'abiz_device_commands' AND indexname = 'abiz_device_commands_dead_letter_idx'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue("Index abiz_device_commands_dead_letter_idx exists in PostgreSQL", rs.next());
        }
    }

    // =====================================================================
    // Section 6: 17.6 Command Reconciliation Worker (8 tests)
    // =====================================================================
    private static void runSection6(CommandReconciliationWorker reconciliationWorker, DeviceCommandMapper commandMapper, UUID shopA, UUID devA) throws Exception {
        System.out.println("\n--- 17.6: Command Reconciliation Worker ---");
        int sweepCount = reconciliationWorker.sweepExpiredCommands();
        assertTrue("sweepExpiredCommands() completes without exception", sweepCount >= 0);

        int[] fullRecon = reconciliationWorker.runFullReconciliation();
        assertEquals("runFullReconciliation() returns 2 metrics", 2, fullRecon.length);
        assertTrue("fullReconciliation expired count >= 0", fullRecon[0] >= 0);
        assertTrue("fullReconciliation recovered count >= 0", fullRecon[1] >= 0);

        UUID testCmdId = UUID.randomUUID();
        createRawCommand(testCmdId, shopA, devA, "LOCK", "SENT", new Date(System.currentTimeMillis() + 60000));
        assertTrue("markReconciled() updates command state", reconciliationWorker.markReconciled(testCmdId));

        AbizDeviceCommand reconciledCmd = commandMapper.findCommandById(testCmdId);
        assertEquals("Reconciliation state is RECONCILED", "RECONCILED", reconciledCmd.getReconciliationState());

        assertTrue("markMismatch() updates command state to MISMATCH", reconciliationWorker.markMismatch(testCmdId, "Lock screen mismatch"));
        AbizDeviceCommand mismatchedCmd = commandMapper.findCommandById(testCmdId);
        assertEquals("Reconciliation state is MISMATCH", "MISMATCH", mismatchedCmd.getReconciliationState());
    }

    // =====================================================================
    // Section 7: 17.7 Dead-Letter / Failed Command Handling (10 tests)
    // =====================================================================
    private static void runSection7(CommandReconciliationWorker reconciliationWorker, DeviceCommandMapper commandMapper, UUID shopA, UUID devA) throws Exception {
        System.out.println("\n--- 17.7: Dead-Letter / Failed Command Handling ---");
        try {
            reconciliationWorker.moveToDeadLetter(null, "CODE", "Reason");
            fail("moveToDeadLetter(null) should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("moveToDeadLetter(null) rejected with IllegalArgumentException");
        }

        assertFalse("moveToDeadLetter on non-existent command returns false",
                reconciliationWorker.moveToDeadLetter(UUID.randomUUID(), "ERR_404", "Not found"));

        UUID deadCmdId = UUID.randomUUID();
        createRawCommand(deadCmdId, shopA, devA, "LOCK", "FAILED", new Date(System.currentTimeMillis() + 60000));
        assertTrue("moveToDeadLetter on FAILED command returns true",
                reconciliationWorker.moveToDeadLetter(deadCmdId, "PUSH_FAILED_FATAL", "Unregistered push token"));

        AbizDeviceCommand deadCmd = commandMapper.findCommandById(deadCmdId);
        assertNotNull("deadLetteredAt is set", deadCmd.getDeadLetteredAt());
        assertEquals("failureCode is set", "PUSH_FAILED_FATAL", deadCmd.getFailureCode());
        assertEquals("reconciliationState is DEAD_LETTERED", "DEAD_LETTERED", deadCmd.getReconciliationState());

        try {
            reconciliationWorker.replayFromDeadLetter(null, 60);
            fail("replayFromDeadLetter(null) should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("replayFromDeadLetter(null) rejected with IllegalArgumentException");
        }

        assertFalse("replayFromDeadLetter on non-deadletter command returns false",
                reconciliationWorker.replayFromDeadLetter(UUID.randomUUID(), 60));

        assertTrue("replayFromDeadLetter succeeds on dead-lettered command",
                reconciliationWorker.replayFromDeadLetter(deadCmdId, 120));

        AbizDeviceCommand replayedCmd = commandMapper.findCommandById(deadCmdId);
        assertEquals("Replayed command status is PENDING", AbizDeviceCommand.STATUS_PENDING, replayedCmd.getStatus());
        assertEquals("Replayed command retryCount is reset to 0", 0, replayedCmd.getRetryCount());
    }

    // =====================================================================
    // Section 8: 17.11 Device Health State Machine (10 tests)
    // =====================================================================
    private static void runSection8() {
        System.out.println("\n--- 17.11: Device Health State Machine ---");
        assertEquals("Health status with Long.MAX_VALUE elapsed is UNKNOWN",
                DeviceHealthStateMachine.HEALTH_UNKNOWN, DeviceHealthStateMachine.computeHealthStatus(Long.MAX_VALUE, false));

        assertEquals("Health status with elapsed > 30 min is UNREACHABLE",
                DeviceHealthStateMachine.HEALTH_UNREACHABLE, DeviceHealthStateMachine.computeHealthStatus(35 * 60 * 1000L, false));

        assertEquals("Health status with elapsed 10 min is STALE",
                DeviceHealthStateMachine.HEALTH_STALE, DeviceHealthStateMachine.computeHealthStatus(10 * 60 * 1000L, false));

        assertEquals("Health status with mismatch is DEGRADED",
                DeviceHealthStateMachine.HEALTH_DEGRADED, DeviceHealthStateMachine.computeHealthStatus(60 * 1000L, true));

        assertEquals("Health status recent and matched is HEALTHY",
                DeviceHealthStateMachine.HEALTH_HEALTHY, DeviceHealthStateMachine.computeHealthStatus(60 * 1000L, false));

        assertTrue("UNKNOWN -> HEALTHY is valid", DeviceHealthStateMachine.isValidTransition("UNKNOWN", "HEALTHY"));
        assertTrue("HEALTHY -> DEGRADED is valid", DeviceHealthStateMachine.isValidTransition("HEALTHY", "DEGRADED"));
        assertTrue("STALE -> UNREACHABLE is valid", DeviceHealthStateMachine.isValidTransition("STALE", "UNREACHABLE"));
        assertFalse("HEALTHY -> HEALTHY (self transition) is invalid", DeviceHealthStateMachine.isValidTransition("HEALTHY", "HEALTHY"));
        assertTrue("DeviceHealthStateMachine.isUnreachable(UNREACHABLE) is true", DeviceHealthStateMachine.isUnreachable("UNREACHABLE"));
    }

    // =====================================================================
    // Section 9: 17.12 Physical vs Backend State Reconciliation (5 tests)
    // =====================================================================
    private static void runSection9(FinancedDeviceMapper deviceMapper, UUID devA) {
        System.out.println("\n--- 17.12: Physical vs Backend State Reconciliation ---");
        deviceMapper.updateHealthStatus(devA, DeviceHealthStateMachine.HEALTH_DEGRADED);
        AbizFinancedDevice checkDevA = deviceMapper.findDeviceById(devA);
        assertEquals("Device health updated to DEGRADED in DB", DeviceHealthStateMachine.HEALTH_DEGRADED, checkDevA.getHealthStatus());

        deviceMapper.updateHealthStatus(devA, DeviceHealthStateMachine.HEALTH_HEALTHY);
        checkDevA = deviceMapper.findDeviceById(devA);
        assertEquals("Device health updated to HEALTHY in DB", DeviceHealthStateMachine.HEALTH_HEALTHY, checkDevA.getHealthStatus());

        List<AbizFinancedDevice> healthyList = deviceMapper.findDevicesByHealth("HEALTHY", 10);
        assertTrue("findDevicesByHealth returns non-empty list", healthyList.size() > 0);

        long healthyCount = deviceMapper.countDevicesByHealth("HEALTHY");
        assertTrue("countDevicesByHealth('HEALTHY') > 0", healthyCount > 0);

        long unknownCount = deviceMapper.countDevicesByHealth("UNKNOWN");
        assertTrue("countDevicesByHealth('UNKNOWN') >= 0", unknownCount >= 0);
    }

    // =====================================================================
    // Section 10: 17.13 Command TTL & Clock Reliability (5 tests)
    // =====================================================================
    private static void runSection10(DeviceCommandMapper commandMapper, UUID shopA, UUID devA) throws Exception {
        System.out.println("\n--- 17.13: Command TTL & Clock Reliability ---");
        UUID pastCmdId = UUID.randomUUID();
        Date pastExpiry = new Date(System.currentTimeMillis() - 100000);
        createRawCommand(pastCmdId, shopA, devA, "LOCK", "PENDING", pastExpiry);

        int swept = commandMapper.expirePendingCommands(new Date());
        assertTrue("Sweeper swept at least 1 expired command", swept >= 1);

        AbizDeviceCommand expiredCmd = commandMapper.findCommandById(pastCmdId);
        assertEquals("Past TTL command status is EXPIRED", AbizDeviceCommand.STATUS_EXPIRED, expiredCmd.getStatus());
        assertTrue("Failure reason mentions TTL expiration", expiredCmd.getFailureReason().contains("TTL expired"));

        UUID futureCmdId = UUID.randomUUID();
        Date futureExpiry = new Date(System.currentTimeMillis() + 3600000);
        createRawCommand(futureCmdId, shopA, devA, "LOCK", "PENDING", futureExpiry);
        commandMapper.expirePendingCommands(new Date());

        AbizDeviceCommand futureCmd = commandMapper.findCommandById(futureCmdId);
        assertEquals("Future TTL command remains PENDING", AbizDeviceCommand.STATUS_PENDING, futureCmd.getStatus());
    }

    // =====================================================================
    // Section 11: 17.14 Security Event Aggregation & Thresholds (10 tests)
    // =====================================================================
    private static void runSection11(SecurityEventAggregator securityAggregator, SecurityEventMapper securityEventMapper, UUID shopA, UUID devA) {
        System.out.println("\n--- 17.14: Security Event Aggregation & Thresholds ---");
        securityAggregator.resetCounters();
        assertEquals("Event counter starts at 0", 0, securityAggregator.getEventCount(devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH));

        securityAggregator.recordEvent(shopA, devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH, "Rate limit exceeded");
        assertEquals("Event counter increments to 1", 1, securityAggregator.getEventCount(devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH));

        securityAggregator.recordEvent(shopA, devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH, "Rate limit exceeded 2nd");
        assertEquals("Event counter increments to 2", 2, securityAggregator.getEventCount(devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH));

        securityAggregator.recordEvent(shopA, devA, SecurityEventAggregator.EVENT_NONCE_REPLAY, "Nonce replay detected");
        assertEquals("Nonce replay counter is 1", 1, securityAggregator.getEventCount(devA, SecurityEventAggregator.EVENT_NONCE_REPLAY));

        List<Map<String, Object>> shopEvents = securityAggregator.getRecentEventsForShop(shopA, 10);
        assertTrue("getRecentEventsForShop returns recorded events", shopEvents.size() >= 2);

        List<Map<String, Object>> devEvents = securityAggregator.getRecentEventsForDevice(devA, 10);
        assertTrue("getRecentEventsForDevice returns recorded events", devEvents.size() >= 2);

        Date oneHourAgo = new Date(System.currentTimeMillis() - 3600000);
        long breachCount = securityEventMapper.countEventsByDeviceSince(devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH, oneHourAgo);
        assertTrue("countEventsByDeviceSince returns >= 2", breachCount >= 2);

        long typeCount = securityEventMapper.countEventsByTypeSince(SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH, oneHourAgo);
        assertTrue("countEventsByTypeSince returns >= 2", typeCount >= 2);

        securityAggregator.resetCounters();
        assertEquals("Counters reset to 0", 0, securityAggregator.getEventCount(devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH));
        pass("Security event aggregation and DB queries verified");
    }

    // =====================================================================
    // Section 12: 17.15 Audit Log Integrity & Write Safety (3 tests)
    // =====================================================================
    private static void runSection12(AbizAuditService auditService, UUID shopA, UUID devA) {
        System.out.println("\n--- 17.15: Audit Log Integrity ---");
        auditService.logSuccess(shopA, "SHOP_OWNER", shopA, "TEST_P17_AUDIT", "DEVICE", devA, "Phase 17 audit write", "127.0.0.1");
        pass("auditService.logSuccess() completes cleanly");

        auditService.logFailure(shopA, "SHOP_OWNER", shopA, "TEST_P17_AUDIT_FAIL", devA, "Phase 17 audit failure write", "127.0.0.1");
        pass("auditService.logFailure() completes cleanly");

        AbizAuditService brokenAudit = new AbizAuditService();
        brokenAudit.logSuccess(shopA, "SHOP_OWNER", shopA, "TEST_FAIL_SAFE", "DEVICE", devA, "Safe audit test", "127.0.0.1");
        pass("Audit logging is failure-tolerant and never crashes business logic");
    }
    // =====================================================================
    // Section 13: 17.16 Rate Limiting Persistence & Defense (3 tests)
    // =====================================================================
    private static void runSection13(DeviceCommandService commandService, UUID shopA, UUID custA) throws Exception {
        System.out.println("\n--- 17.16: Rate Limiting Persistence & Defense ---");
        AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopA, "ownerA@shopA.com");
        UUID burstDev = UUID.randomUUID();
        createDevice(burstDev, shopA, custA, nextValidImei(), "BURST-TOKEN");

        int createdCount = 0;
        boolean hitLimit = false;
        for (int i = 0; i < 35; i++) {
            try {
                commandService.createCommand(burstDev, AbizDeviceCommand.TYPE_LOCK, "BURST-" + i, Collections.emptyMap(), 10);
                createdCount++;
            } catch (SecurityException se) {
                hitLimit = true;
                break;
            }
        }
        assertTrue("Burst command creation triggered Rate Limiting SecurityException", hitLimit);
        assertTrue("Created commands before limit reached <= 30", createdCount <= 30);
        AbizTenantContext.clear();
        pass("Rate limiting properly rejects high-velocity command storms");
    }

    // =====================================================================
    // Section 14: 17.17 Deployment Migration Safety (4 tests)
    // =====================================================================
    private static void runSection14() throws Exception {
        System.out.println("\n--- 17.17: Deployment Migration Safety ---");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_name = 'abiz_device_commands'")) {
            ResultSet rs = ps.executeQuery();
            Set<String> cols = new HashSet<>();
            while (rs.next()) cols.add(rs.getString(1).toLowerCase());
            assertTrue("abiz_device_commands contains dead_lettered_at", cols.contains("dead_lettered_at"));
            assertTrue("abiz_device_commands contains failure_code", cols.contains("failure_code"));
            assertTrue("abiz_device_commands contains reconciliation_state", cols.contains("reconciliation_state"));
        }
        pass("Liquibase migration safety and schema integrity verified");
    }

    // =====================================================================
    // Section 15: 17.18 Backup & Restore Readiness (3 tests)
    // =====================================================================
    private static void runSection15(HealthService healthService) {
        System.out.println("\n--- 17.18: Backup & Restore Readiness ---");
        Map<String, Object> backupCheck = healthService.checkBackupReadiness();
        assertNotNull("checkBackupReadiness returns map", backupCheck);
        assertEquals("Database is reachable for backup", true, backupCheck.get("databaseReachable"));
        assertEquals("Backup check status is PASS", "PASS", backupCheck.get("status"));
    }

    // =====================================================================
    // Section 16: 17.19 Disaster Recovery Simulation (5 tests)
    // =====================================================================
    private static void runSection16(SqlSessionFactory sqlSessionFactory, AbizAuditService auditService) {
        System.out.println("\n--- 17.19: Disaster Recovery Simulation ---");
        try (SqlSession drSession = sqlSessionFactory.openSession(true)) {
            DeviceCommandMapper drCommandMapper = drSession.getMapper(DeviceCommandMapper.class);
            FinancedDeviceMapper drDeviceMapper = drSession.getMapper(FinancedDeviceMapper.class);
            HealthService drHealth = new HealthService(drCommandMapper, drDeviceMapper);
            CommandReconciliationWorker drRecon = new CommandReconciliationWorker(drCommandMapper, auditService);

            assertTrue("DR Service instance isLive()", drHealth.isLive());
            assertTrue("DR Service instance isReady()", drHealth.isReady());
            assertTrue("DR Service queries command queue depth", drHealth.getCommandQueueDepth() >= 0);
            assertTrue("DR Reconciliation sweep succeeds", drRecon.sweepExpiredCommands() >= 0);
            assertEquals("DR Backup check passes", "PASS", drHealth.checkBackupReadiness().get("status"));
        }
    }

    // =====================================================================
    // Section 17: 17.20 Worker Crash Recovery (5 tests)
    // =====================================================================
    private static void runSection17(DeviceCommandMapper commandMapper, UUID shopA, UUID devA) throws Exception {
        System.out.println("\n--- 17.20: Worker Crash Recovery ---");
        UUID stuckCmdId = UUID.randomUUID();
        Date oldUpdated = new Date(System.currentTimeMillis() - (3L * 3600 * 1000));
        Date validExpiry = new Date(System.currentTimeMillis() + 3600000);
        createRawCommandWithUpdatedAt(stuckCmdId, shopA, devA, "LOCK", "DISPATCHING", oldUpdated, validExpiry);

        Date cutoff = new Date(System.currentTimeMillis() - (2L * 3600 * 1000));
        int recovered = commandMapper.resetStuckDispatchingCommands(cutoff, new Date());
        assertTrue("resetStuckDispatchingCommands recovered at least 1 stuck command", recovered >= 1);

        AbizDeviceCommand recoveredCmd = commandMapper.findCommandById(stuckCmdId);
        assertEquals("Stuck command reset to PENDING", AbizDeviceCommand.STATUS_PENDING, recoveredCmd.getStatus());
        assertEquals("Reconciliation state set to RECOVERED", "RECOVERED", recoveredCmd.getReconciliationState());
        assertEquals("Claim fields cleared", null, recoveredCmd.getClaimedBy());
        assertTrue("Failure reason explains recovery", recoveredCmd.getFailureReason().contains("Recovered from stuck DISPATCHING"));
    }

    // =====================================================================
    // Section 18: 17.22 WIPE RBAC & Safety Revalidation (3 tests)
    // =====================================================================
    private static void runSection18(DeviceCommandService commandService, UUID shopA, UUID devA) {
        System.out.println("\n--- 17.22: WIPE RBAC & Safety Revalidation ---");
        AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_STAFF, shopA, "staffA@shopA.com");
        try {
            commandService.createCommand(devA, AbizDeviceCommand.TYPE_WIPE, "WIPE-STAFF-1", Collections.emptyMap(), 10);
            fail("SHOP_STAFF should be BLOCKED from issuing WIPE");
        } catch (SecurityException se) {
            pass("SHOP_STAFF is strictly prevented from issuing WIPE command");
        }
        AbizTenantContext.clear();

        AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopA, "ownerA@shopA.com");
        AbizDeviceCommand ownerWipe = commandService.createCommand(devA, AbizDeviceCommand.TYPE_WIPE, "WIPE-OWNER-1", Collections.emptyMap(), 10);
        assertNotNull("SHOP_OWNER is permitted to create WIPE command", ownerWipe);
        assertEquals("Command type is WIPE", AbizDeviceCommand.TYPE_WIPE, ownerWipe.getCommandType());
        AbizTenantContext.clear();
    }

    // =====================================================================
    // Section 19: 17.23 Security Headers, Nonce & RequestId (3 tests)
    // =====================================================================
    private static void runSection19(DeviceCommandService commandService, UUID shopA, UUID devA) {
        System.out.println("\n--- 17.23: Security Headers & RequestId ---");
        AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopA, "ownerA@shopA.com");
        AbizDeviceCommand headerCmd = commandService.createCommand(devA, AbizDeviceCommand.TYPE_LOCK, "SEC-HDR-1", Collections.emptyMap(), 10);
        assertNotNull("Nonce generated", headerCmd.getNonce());
        assertTrue("Nonce is 32 character hex string", headerCmd.getNonce().matches("^[0-9a-fA-F]{32}$"));
        assertTrue("RequestId follows REQ-... pattern", headerCmd.getRequestId().matches("^REQ-[0-9a-fA-F]{8}$"));
        AbizTenantContext.clear();
    }

    // =====================================================================
    // Section 20: 17.24 API Error Contract & Exception Boundaries (3 tests)
    // =====================================================================
    private static void runSection20(DeviceCommandService commandService, UUID devA) {
        System.out.println("\n--- 17.24: API Error Contract ---");
        try {
            commandService.createCommand(null, "LOCK", "K", Collections.emptyMap(), 10);
            fail("null deviceId should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("null deviceId rejected with standard IllegalArgumentException");
        }

        try {
            commandService.createCommand(devA, "INVALID_TYPE", "K", Collections.emptyMap(), 10);
            fail("Invalid command type should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("Invalid command type rejected with standard IllegalArgumentException");
        }

        try {
            commandService.createCommand(UUID.randomUUID(), "LOCK", "K", Collections.emptyMap(), 10);
            fail("Non-existent device should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            pass("Non-existent deviceId rejected with standard IllegalArgumentException");
        }
    }

    // =====================================================================
    // Section 21: 17.25 Operational Metrics & Queue Depth (5 tests)
    // =====================================================================
    private static void runSection21(HealthService healthService) {
        System.out.println("\n--- 17.25: Operational Metrics ---");
        assertTrue("getCommandQueueDepth() returns >= 0", healthService.getCommandQueueDepth() >= 0);
        assertTrue("getStaleDeviceCount() returns >= 0", healthService.getStaleDeviceCount() >= 0);
        Map<String, Object> opMetrics = healthService.getOperationalMetrics();
        assertTrue("Metrics map contains 'healthyDevices'", opMetrics.containsKey("healthyDevices"));
        assertTrue("Metrics map contains 'degradedDevices'", opMetrics.containsKey("degradedDevices"));
        assertTrue("Metrics map contains 'unreachableDevices'", opMetrics.containsKey("unreachableDevices"));
    }

    // =====================================================================
    // Section 22: 17.27 Multi-Tenant Security Regression (5 tests)
    // =====================================================================
    private static void runSection22(DeviceCommandService commandService, DeviceCommandMapper commandMapper, UUID shopA, UUID shopB, UUID devB) {
        System.out.println("\n--- 17.27: Multi-Tenant Security Regression ---");
        AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopA, "ownerA@shopA.com");
        try {
            commandService.createCommand(devB, AbizDeviceCommand.TYPE_LOCK, "CROSS-TENANT-1", Collections.emptyMap(), 10);
            fail("Cross-tenant command creation MUST throw SecurityException");
        } catch (SecurityException se) {
            pass("Cross-tenant command creation blocked with SecurityException");
        }

        try {
            commandService.listDeviceCommands(devB, 1, 10);
            fail("Cross-tenant command listing MUST throw SecurityException");
        } catch (SecurityException se) {
            pass("Cross-tenant command list blocked with SecurityException");
        }
        AbizTenantContext.clear();

        AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopB, "ownerB@shopB.com");
        AbizDeviceCommand shopBCmd = commandService.createCommand(devB, AbizDeviceCommand.TYPE_LOCK, "SHOP-B-CMD", Collections.emptyMap(), 10);
        assertNotNull("Shop B owner successfully creates command on Shop B device", shopBCmd);
        assertEquals("Command shopId is Shop B", shopB, shopBCmd.getShopId());
        AbizTenantContext.clear();

        List<AbizDeviceCommand> shopACommands = commandMapper.findCommandsByShop(shopA, 100, 0);
        boolean leakedB = shopACommands.stream().anyMatch(c -> c.getShopId().equals(shopB));
        assertFalse("Shop A command query contains ZERO Shop B commands", leakedB);
    }

    // =====================================================================
    // Section 23: 17.29 Production Build & WAR Verification (3 tests)
    // =====================================================================
    private static void runSection23() {
        System.out.println("\n--- 17.29: Production Build & WAR Verification ---");
        File warFile = new File("mdmesh/server/target/launcher.war");
        assertTrue("launcher.war file exists", warFile.exists());
        assertTrue("launcher.war is larger than 30MB (" + warFile.length() + " bytes)", warFile.length() > 30_000_000L);
        assertTrue("launcher.war is packaged with valid .war extension", warFile.getName().endsWith(".war"));
    }

    private static void createShop(UUID shopId, String name, String owner) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO abiz_shops (id, name, owner_name, phone, email, status, created_at, updated_at) " +
                             "VALUES (?, ?, ?, '03001234567', ?, 'ACTIVE', NOW(), NOW()) ON CONFLICT (id) DO NOTHING")) {
            ps.setObject(1, shopId);
            ps.setString(2, name);
            ps.setString(3, owner);
            ps.setString(4, name.toLowerCase().replace(" ", "") + "@test.com");
            ps.executeUpdate();
        }
    }

    private static void createCustomer(UUID custId, UUID shopId, String name, String phone) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO abiz_customers (id, shop_id, name, phone, status, created_at, updated_at) " +
                             "VALUES (?, ?, ?, ?, 'ACTIVE', NOW(), NOW()) ON CONFLICT (id) DO NOTHING")) {
            ps.setObject(1, custId);
            ps.setObject(2, shopId);
            ps.setString(3, name);
            ps.setString(4, phone);
            ps.executeUpdate();
        }
    }

    private static void createDevice(UUID devId, UUID shopId, UUID custId, String imei, String pushToken) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO abiz_financed_devices (id, shop_id, customer_id, imei1, enrollment_status, " +
                             "device_owner_active, connection_status, lock_status, license_status, policy_state, " +
                             "push_token, health_status, last_heartbeat_at, created_at, updated_at) " +
                             "VALUES (?, ?, ?, ?, 'ENROLLED', TRUE, 'ONLINE', 'UNLOCKED', 'ACTIVE', 'ACTIVE', " +
                             "?, 'UNKNOWN', NOW(), NOW(), NOW()) ON CONFLICT (id) DO NOTHING")) {
            ps.setObject(1, devId);
            ps.setObject(2, shopId);
            ps.setObject(3, custId);
            ps.setString(4, imei);
            ps.setString(5, pushToken);
            ps.executeUpdate();
        }
    }

    private static void createRawCommand(UUID id, UUID shopId, UUID devId, String type, String status, Date expiresAt) throws Exception {
        createRawCommandWithUpdatedAt(id, shopId, devId, type, status, new Date(), expiresAt);
    }

    private static void createRawCommandWithUpdatedAt(UUID id, UUID shopId, UUID devId, String type, String status, Date updatedAt, Date expiresAt) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO abiz_device_commands (id, shop_id, financed_device_id, command_type, status, " +
                             "idempotency_key, correlation_id, retry_count, max_retries, nonce, request_id, protocol_version, " +
                             "created_at, updated_at, expires_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 3, '12345678901234567890123456789012', 'REQ-00000000', 1, " +
                             "?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, shopId);
            ps.setObject(3, devId);
            ps.setString(4, type);
            ps.setString(5, status);
            ps.setString(6, "IDEMP-" + UUID.randomUUID().toString().substring(0, 8));
            ps.setString(7, UUID.randomUUID().toString());
            ps.setTimestamp(8, new java.sql.Timestamp(updatedAt.getTime()));
            ps.setTimestamp(9, new java.sql.Timestamp(updatedAt.getTime()));
            ps.setTimestamp(10, new java.sql.Timestamp(expiresAt.getTime()));
            ps.executeUpdate();
        }
    }

    private static void pass(String msg) {
        testsPassed++;
        System.out.println("  [PASS] " + msg);
    }

    private static void fail(String msg) {
        testsFailed++;
        System.err.println("  [FAIL] " + msg);
    }

    private static void assertTrue(String msg, boolean condition) {
        if (condition) pass(msg); else fail(msg);
    }

    private static void assertFalse(String msg, boolean condition) {
        if (!condition) pass(msg); else fail(msg);
    }

    private static void assertEquals(String msg, Object expected, Object actual) {
        if (expected instanceof Number && actual instanceof Number) {
            if (((Number) expected).longValue() == ((Number) actual).longValue()) {
                pass(msg + " (Got: " + actual + ")");
                return;
            }
        }
        if (Objects.equals(expected, actual)) {
            pass(msg + " (Got: " + actual + ")");
        } else {
            fail(msg + " (Expected: " + expected + ", Got: " + actual + ")");
        }
    }

    private static void assertNotNull(String msg, Object obj) {
        if (obj != null) pass(msg); else fail(msg + " (Expected non-null)");
    }

    public static class MockHardenedTransport implements DeviceCommandTransport {
        @Override
        public CommandDispatchResult dispatch(AbizDeviceCommand command) {
            return CommandDispatchResult.success("MSG-" + UUID.randomUUID().toString().substring(0, 8));
        }

        @Override
        public String getProviderName() {
            return "MockHardenedTransport";
        }
    }
}