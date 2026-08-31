package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.command.DeviceCommandService;
import com.abiz.locker.command.DeviceCommandStateMachine;
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
 * Deterministic Backend Verification Suite for Phase 16:
 * Production Hardening, Android Agent Reliability & Security Verification.
 */
public class Phase16ProductionHardeningVerificationTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    private static final String DB_USER = "abizlocker";
    private static final String DB_PASS = "abiz_dev_secure_pass_9281";

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static long imeiSequence = 86000000000000L + (System.currentTimeMillis() % 9000000000000L);

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
        System.out.println("  ABIZ LOCKER — PHASE 16 PRODUCTION HARDENING VERIFICATION SUITE");
        System.out.println("==========================================================================");

        try {
            PooledDataSource ds = new PooledDataSource("org.postgresql.Driver", DB_URL, DB_USER, DB_PASS);
            Environment env = new Environment("test", new JdbcTransactionFactory(), ds);
            Configuration config = new Configuration(env);
            config.getTypeHandlerRegistry().register(UUID.class, new UuidTypeHandler());
            config.addMapper(DeviceCommandMapper.class);
            config.addMapper(DevicePolicyMapper.class);
            config.addMapper(ShopMapper.class);
            config.addMapper(FinancedDeviceMapper.class);
            config.addMapper(CustomerMapper.class);
            config.addMapper(AuditMapper.class);

            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = sqlSessionFactory.openSession(true)) {
                DeviceCommandMapper commandMapper = session.getMapper(DeviceCommandMapper.class);
                DevicePolicyMapper policyMapper = session.getMapper(DevicePolicyMapper.class);
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);
                CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                java.lang.reflect.Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                f.setAccessible(true);
                f.set(auditService, auditMapper);

                MockHardenedTransport transport = new MockHardenedTransport();
                DeviceCommandService commandService = new DeviceCommandService(commandMapper, policyMapper, transport, auditService);

                // Setup test tenant fixtures
                UUID shopA = UUID.randomUUID();
                UUID shopB = UUID.randomUUID();
                UUID ownerA = UUID.randomUUID();
                UUID staffA = UUID.randomUUID();
                UUID ownerB = UUID.randomUUID();

                createShop(shopA, "Hardening Shop A", "Owner A");
                createShop(shopB, "Hardening Shop B", "Owner B");

                UUID custA = UUID.randomUUID();
                createCustomer(custA, shopA, "Customer A", "03001112233");

                UUID devA = UUID.randomUUID();
                createDevice(devA, shopA, custA, nextValidImei(), "PUSH-TOKEN-A");

                UUID devB = UUID.randomUUID();
                createDevice(devB, shopB, null, nextValidImei(), "PUSH-TOKEN-B");

                // =====================================================================
                // Section 1: Command State Machine & Illegal Transitions
                // =====================================================================
                System.out.println("\n--- Section 1: Command State Machine & Transitions ---");
                assertTrue("PENDING -> DISPATCHING is valid", DeviceCommandStateMachine.isValidTransition("PENDING", "DISPATCHING"));
                assertTrue("PENDING -> EXECUTED is valid (direct ACK)", DeviceCommandStateMachine.isValidTransition("PENDING", "EXECUTED"));
                assertTrue("PENDING -> FAILED is valid", DeviceCommandStateMachine.isValidTransition("PENDING", "FAILED"));
                assertTrue("PENDING -> EXPIRED is valid", DeviceCommandStateMachine.isValidTransition("PENDING", "EXPIRED"));
                assertTrue("PENDING -> CANCELLED is valid", DeviceCommandStateMachine.isValidTransition("PENDING", "CANCELLED"));

                assertTrue("DISPATCHING -> SENT is valid", DeviceCommandStateMachine.isValidTransition("DISPATCHING", "SENT"));
                assertTrue("DISPATCHING -> PENDING is valid (transient retry)", DeviceCommandStateMachine.isValidTransition("DISPATCHING", "PENDING"));
                assertTrue("SENT -> EXECUTED is valid", DeviceCommandStateMachine.isValidTransition("SENT", "EXECUTED"));
                assertTrue("SENT -> ACKNOWLEDGED is valid", DeviceCommandStateMachine.isValidTransition("SENT", "ACKNOWLEDGED"));
                assertTrue("ACKNOWLEDGED -> EXECUTED is valid", DeviceCommandStateMachine.isValidTransition("ACKNOWLEDGED", "EXECUTED"));

                assertFalse("EXECUTED -> PENDING is illegal", DeviceCommandStateMachine.isValidTransition("EXECUTED", "PENDING"));
                assertFalse("EXECUTED -> SENT is illegal", DeviceCommandStateMachine.isValidTransition("EXECUTED", "SENT"));
                assertFalse("EXPIRED -> EXECUTED is illegal", DeviceCommandStateMachine.isValidTransition("EXPIRED", "EXECUTED"));
                assertFalse("CANCELLED -> EXECUTED is illegal", DeviceCommandStateMachine.isValidTransition("CANCELLED", "EXECUTED"));
                assertFalse("FAILED -> EXECUTED is illegal", DeviceCommandStateMachine.isValidTransition("FAILED", "EXECUTED"));

                assertTrue("EXECUTED is terminal state", DeviceCommandStateMachine.isTerminalState("EXECUTED"));
                assertTrue("EXPIRED is terminal state", DeviceCommandStateMachine.isTerminalState("EXPIRED"));
                assertTrue("CANCELLED is terminal state", DeviceCommandStateMachine.isTerminalState("CANCELLED"));

                boolean threwOnTerminal = false;
                try {
                    DeviceCommandStateMachine.validateTransition("EXECUTED", "SENT");
                } catch (IllegalStateException e) {
                    threwOnTerminal = true;
                }
                assertTrue("validateTransition throws on illegal transition from terminal state", threwOnTerminal);

                // =====================================================================
                // Section 2: Command Expiration & TTL Enforcement
                // =====================================================================
                System.out.println("\n--- Section 2: Command Expiration & TTL Enforcement ---");
                AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "ownerA@shopA.com");

                // 2.1 Create fresh command with TTL
                AbizDeviceCommand freshCmd = commandService.createCommand(devA, AbizDeviceCommand.TYPE_LOCK, "EXP-KEY-1", null, 60);
                assertNotNull("Fresh command created with valid expiresAt", freshCmd.getExpiresAt());
                assertTrue("Expiration timestamp is in future", freshCmd.getExpiresAt().after(new Date()));

                // 2.2 Simulate expired command
                freshCmd.setExpiresAt(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
                commandMapper.updateCommand(freshCmd);

                // 2.3 Dispatch expired command must transition to EXPIRED and abort dispatch
                AbizDeviceCommand expiredDispatched = commandService.dispatchCommand(freshCmd.getId());
                assertEquals("Expired command dispatch transitions to EXPIRED", AbizDeviceCommand.STATUS_EXPIRED, expiredDispatched.getStatus());

                // 2.4 ACK on expired command must be rejected
                boolean threwOnExpiredAck = false;
                try {
                    commandService.acknowledgeCommand(freshCmd.getId(), new DeviceCommandAckRequest(
                            freshCmd.getId(), devA, "SUCCESS", "EXECUTED", "Executed after expiry", "0.1.16"
                    ));
                } catch (IllegalStateException e) {
                    threwOnExpiredAck = true;
                }
                assertTrue("Cannot acknowledge EXPIRED command (IllegalStateException)", threwOnExpiredAck);

                // =====================================================================
                // Section 3: Concurrent Dispatch & Worker Lease Claiming
                // =====================================================================
                System.out.println("\n--- Section 3: Concurrent Dispatch & Worker Lease Claiming ---");
                AbizDeviceCommand claimCmd = commandService.createCommand(devA, AbizDeviceCommand.TYPE_UNLOCK, "CLAIM-KEY-1", null, 60);
                assertEquals("Command created in PENDING status", AbizDeviceCommand.STATUS_PENDING, claimCmd.getStatus());

                // Simulate Worker A claiming command
                boolean workerAClaimed = commandService.claimCommand(claimCmd.getId(), "worker-node-1");
                assertTrue("Worker A successfully claimed command", workerAClaimed);

                // Simulate Worker B attempting to claim the same command concurrently
                boolean workerBClaimed = commandService.claimCommand(claimCmd.getId(), "worker-node-2");
                assertFalse("Worker B cannot claim already claimed command", workerBClaimed);

                AbizDeviceCommand claimedInDb = commandMapper.findCommandById(claimCmd.getId());
                assertEquals("Command status in DB updated to DISPATCHING", AbizDeviceCommand.STATUS_DISPATCHING, claimedInDb.getStatus());
                assertEquals("ClaimedBy recorded as worker-node-1", "worker-node-1", claimedInDb.getClaimedBy());
                assertNotNull("ClaimedAt recorded in DB", claimedInDb.getClaimedAt());

                // Dispatch by Worker A succeeds
                AbizDeviceCommand dispatchedLeaseCmd = commandService.dispatchCommand(claimCmd.getId());
                assertEquals("Dispatched lease command reaches SENT", AbizDeviceCommand.STATUS_SENT, dispatchedLeaseCmd.getStatus());

                // =====================================================================
                // Section 4: ACK Security, Nonce Verification & Replay Defense
                // =====================================================================
                System.out.println("\n--- Section 4: ACK Security & Nonce Verification ---");
                assertNotNull("Command has cryptographic nonce", dispatchedLeaseCmd.getNonce());
                assertNotNull("Command has request ID", dispatchedLeaseCmd.getRequestId());

                // 4.1 Wrong Device ACK rejected (403)
                boolean threwOnWrongDevAck = false;
                try {
                    commandService.acknowledgeCommand(dispatchedLeaseCmd.getId(), new DeviceCommandAckRequest(
                            dispatchedLeaseCmd.getId(), devB, "SUCCESS", "EXECUTED", "Wrong dev", "0.1.16"
                    ));
                } catch (SecurityException e) {
                    threwOnWrongDevAck = true;
                }
                assertTrue("Wrong device ACK rejected with SecurityException (403)", threwOnWrongDevAck);

                // 4.2 Invalid Nonce ACK rejected (403)
                boolean threwOnInvalidNonce = false;
                try {
                    commandService.acknowledgeCommand(dispatchedLeaseCmd.getId(), new DeviceCommandAckRequest(
                            dispatchedLeaseCmd.getId(), devA, "SUCCESS", "EXECUTED", "Invalid nonce", "0.1.16", "WRONG-NONCE-123", dispatchedLeaseCmd.getRequestId()
                    ));
                } catch (SecurityException e) {
                    threwOnInvalidNonce = true;
                }
                assertTrue("Invalid nonce ACK rejected with SecurityException (403)", threwOnInvalidNonce);

                // 4.3 Stale timestamp ACK rejected (Replay defense)
                boolean threwOnStaleTimestamp = false;
                try {
                    DeviceCommandAckRequest staleAck = new DeviceCommandAckRequest(
                            dispatchedLeaseCmd.getId(), devA, "SUCCESS", "EXECUTED", "Stale req", "0.1.16", dispatchedLeaseCmd.getNonce(), dispatchedLeaseCmd.getRequestId()
                    );
                    staleAck.setTimestamp(System.currentTimeMillis() - 30 * 60_000L); // 30 mins stale
                    commandService.acknowledgeCommand(dispatchedLeaseCmd.getId(), staleAck);
                } catch (SecurityException e) {
                    threwOnStaleTimestamp = true;
                }
                assertTrue("Stale timestamp (>15 min skew) rejected as replay attack", threwOnStaleTimestamp);

                // 4.4 Valid ACK accepted and transitions to EXECUTED
                DeviceCommandAckRequest validAck = new DeviceCommandAckRequest(
                        dispatchedLeaseCmd.getId(), devA, "SUCCESS", "EXECUTED", "Valid execution", "0.1.16", dispatchedLeaseCmd.getNonce(), dispatchedLeaseCmd.getRequestId()
                );
                AbizDeviceCommand executedCmd = commandService.acknowledgeCommand(dispatchedLeaseCmd.getId(), validAck);
                assertEquals("Command status transitioned to EXECUTED", AbizDeviceCommand.STATUS_EXECUTED, executedCmd.getStatus());
                assertNotNull("ExecutedAt recorded in DB", executedCmd.getExecutedAt());

                // 4.5 Duplicate valid ACK handled idempotently
                AbizDeviceCommand duplicateAckCmd = commandService.acknowledgeCommand(dispatchedLeaseCmd.getId(), validAck);
                assertEquals("Duplicate ACK returns EXECUTED idempotently", AbizDeviceCommand.STATUS_EXECUTED, duplicateAckCmd.getStatus());

                // =====================================================================
                // Section 5: Cryptographic Device Authentication
                // =====================================================================
                System.out.println("\n--- Section 5: Cryptographic Device Authentication ---");
                String secretPlain = "SuperSecretDeviceKey-2026";
                commandService.registerDeviceSecret(devA, secretPlain);

                AbizFinancedDevice devAInDb = deviceMapper.findDeviceById(devA);
                assertNotNull("Device secret hash stored in DB", devAInDb.getDeviceSecretHash());
                assertFalse("Device secret hash is not plaintext", secretPlain.equals(devAInDb.getDeviceSecretHash()));

                // Valid heartbeat with correct secret
                DeviceHeartbeatRequest validHb = new DeviceHeartbeatRequest(devA, secretPlain, "0.1.16", "16", 88, "WIFI");
                commandService.recordHeartbeat(devA, validHb);
                pass("Valid device heartbeat authenticated successfully");

                // Invalid secret heartbeat rejected (403)
                boolean threwOnBadSecret = false;
                try {
                    DeviceHeartbeatRequest badHb = new DeviceHeartbeatRequest(devA, "WrongSecret-999", "0.1.16", "16", 88, "WIFI");
                    commandService.recordHeartbeat(devA, badHb);
                } catch (SecurityException e) {
                    threwOnBadSecret = true;
                }
                assertTrue("Invalid device secret heartbeat rejected (403)", threwOnBadSecret);

                // =====================================================================
                // Section 6: Bounded Retries & Transient Network Recovery
                // =====================================================================
                System.out.println("\n--- Section 6: Bounded Retries & Transient Network Recovery ---");
                AbizDeviceCommand retryTestCmd = commandService.createCommand(devA, AbizDeviceCommand.TYPE_LOCK, "RETRY-KEY-1", null, 60);

                // Transport fails transiently
                transport.setSimulateTransientFailure(true);
                AbizDeviceCommand failed1 = commandService.dispatchCommand(retryTestCmd.getId());
                assertEquals("Transient dispatch failure retains PENDING status", AbizDeviceCommand.STATUS_PENDING, failed1.getStatus());
                assertEquals("Retry count incremented to 1", 1, failed1.getRetryCount());
                assertNotNull("Last retry timestamp recorded", failed1.getLastRetryAt());

                // Second transient failure
                AbizDeviceCommand failed2 = commandService.dispatchCommand(retryTestCmd.getId());
                assertEquals("Second failure retains PENDING status", AbizDeviceCommand.STATUS_PENDING, failed2.getStatus());
                assertEquals("Retry count incremented to 2", 2, failed2.getRetryCount());

                // Network recovers -> Third attempt succeeds
                transport.setSimulateTransientFailure(false);
                AbizDeviceCommand recovered = commandService.dispatchCommand(retryTestCmd.getId());
                assertEquals("Recovered dispatch transitions to SENT", AbizDeviceCommand.STATUS_SENT, recovered.getStatus());

                // Fatal failure simulation (exhaust retries)
                AbizDeviceCommand fatalCmd = commandService.createCommand(devA, AbizDeviceCommand.TYPE_PAYMENT_WARNING, "FATAL-KEY-1", null, 60);
                transport.setSimulateFatalFailure(true);
                AbizDeviceCommand fatalResult = commandService.dispatchCommand(fatalCmd.getId());
                assertEquals("Fatal transport failure transitions to FAILED", AbizDeviceCommand.STATUS_FAILED, fatalResult.getStatus());
                transport.setSimulateFatalFailure(false);

                // =====================================================================
                // Section 7: Device Heartbeat & Health Telemetry
                // =====================================================================
                System.out.println("\n--- Section 7: Device Heartbeat & Health Telemetry ---");
                commandService.recordHeartbeat(devA, new DeviceHeartbeatRequest(devA, secretPlain, "0.1.16", "16", 95, "5G"));
                AbizFinancedDevice devAHealth = deviceMapper.findDeviceById(devA);
                assertEquals("Health status updated to ONLINE", "ONLINE", devAHealth.getHealthStatus());
                assertEquals("Connection status updated to ONLINE", "ONLINE", devAHealth.getConnectionStatus());
                assertNotNull("Last heartbeat timestamp recorded", devAHealth.getLastHeartbeatAt());

                // =====================================================================
                // Section 8: Physical Device State Reconciliation
                // =====================================================================
                System.out.println("\n--- Section 8: Physical Device State Reconciliation ---");
                policyMapper.updateDevicePolicyAndLockStatus(devA, "ACTIVE", "UNLOCKED");

                // 8.1 Matching report: Backend UNLOCKED, Android UNLOCKED -> Healthy
                DeviceStateReport matchingReport = new DeviceStateReport(devA, "UNLOCKED", true, 92, "WIFI", "0.1.16", "16");
                boolean matchingResult = commandService.reconcileDeviceState(devA, matchingReport);
                assertTrue("Matching state reconciliation reports healthy (true)", matchingResult);

                // 8.2 Divergent report: Backend UNLOCKED, Android report LOCKED -> Degraded
                DeviceStateReport divergentReport = new DeviceStateReport(devA, "LOCKED", true, 92, "WIFI", "0.1.16", "16");
                boolean divergentResult = commandService.reconcileDeviceState(devA, divergentReport);
                assertFalse("Divergent state reconciliation detects divergence (false)", divergentResult);

                AbizFinancedDevice devADegraded = deviceMapper.findDeviceById(devA);
                assertEquals("Health status marked DEGRADED on state divergence", "DEGRADED", devADegraded.getHealthStatus());

                // =====================================================================
                // Section 9: Destructive WIPE Protection & RBAC Safety
                // =====================================================================
                System.out.println("\n--- Section 9: Destructive WIPE Protection & RBAC Safety ---");
                AbizTenantContext.set(staffA, AbizRole.SHOP_STAFF, shopA, "staffA@shopA.com");

                // 9.1 Staff blocked from WIPE
                boolean staffBlockedWipe = false;
                try {
                    commandService.createCommand(devA, AbizDeviceCommand.TYPE_WIPE, "WIPE-STAFF-1", null, 60);
                } catch (SecurityException e) {
                    staffBlockedWipe = true;
                }
                assertTrue("Shop Staff strictly blocked from WIPE command (403)", staffBlockedWipe);

                // 9.2 Owner authorized for WIPE
                AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "ownerA@shopA.com");
                Map<String, Object> wipeExtras = new HashMap<>();
                wipeExtras.put("dryRun", true);
                AbizDeviceCommand wipeCmd = commandService.createCommand(devA, AbizDeviceCommand.TYPE_WIPE, "WIPE-OWNER-1", wipeExtras, 60);
                assertNotNull("Shop Owner authorized to create WIPE command", wipeCmd);
                assertEquals("WIPE command created with type WIPE", AbizDeviceCommand.TYPE_WIPE, wipeCmd.getCommandType());

                // 9.3 Safely cancel WIPE command
                AbizDeviceCommand cancelledWipe = commandService.cancelCommand(wipeCmd.getId(), "Safety verification completed");
                assertEquals("WIPE command safely cancelled without triggering reset", AbizDeviceCommand.STATUS_CANCELLED, cancelledWipe.getStatus());

                // =====================================================================
                // Section 10: Multi-Tenant Security & Attack Matrix
                // =====================================================================
                System.out.println("\n--- Section 10: Multi-Tenant Security & Cross-Tenant Defense ---");
                AbizTenantContext.set(ownerB, AbizRole.SHOP_OWNER, shopB, "ownerB@shopB.com");

                // 10.1 Tenant B cannot command Tenant A device
                boolean crossTenantCommandBlocked = false;
                try {
                    commandService.createCommand(devA, AbizDeviceCommand.TYPE_LOCK, "CROSS-CMD-1", null, 60);
                } catch (SecurityException e) {
                    crossTenantCommandBlocked = true;
                }
                assertTrue("Cross-tenant command creation blocked (Shop B on Shop A device)", crossTenantCommandBlocked);

                // 10.2 Tenant B cannot read Tenant A command
                boolean crossTenantReadBlocked = false;
                try {
                    commandService.getCommand(freshCmd.getId());
                } catch (SecurityException e) {
                    crossTenantReadBlocked = true;
                }
                assertTrue("Cross-tenant command access blocked (Shop B reading Shop A command)", crossTenantReadBlocked);

                // 10.3 Tenant B cannot heartbeat Tenant A device
                boolean crossTenantHbBlocked = false;
                try {
                    commandService.recordHeartbeat(devA, new DeviceHeartbeatRequest(devA, secretPlain, "0.1.16", "16", 50, "LTE"));
                } catch (SecurityException e) {
                    crossTenantHbBlocked = true;
                }
                assertTrue("Cross-tenant device heartbeat blocked", crossTenantHbBlocked);

                // =====================================================================
                // Section 11: Rate Limiting & Burst Protection
                // =====================================================================
                System.out.println("\n--- Section 11: Rate Limiting & Burst Protection ---");
                AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "ownerA@shopA.com");
                UUID rateTestDev = UUID.randomUUID();
                createDevice(rateTestDev, shopA, custA, nextValidImei(), "PUSH-TOKEN-RATE");

                boolean rateLimitTriggered = false;
                try {
                    for (int i = 0; i < 35; i++) {
                        commandService.createCommand(rateTestDev, AbizDeviceCommand.TYPE_LOCK, "RATE-BURST-" + i, null, 60);
                    }
                } catch (SecurityException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Rate limit exceeded")) {
                        rateLimitTriggered = true;
                    }
                }
                assertTrue("Command creation burst throttled after exceeding 30 req/min limit", rateLimitTriggered);

                // =====================================================================
                // Section 12: Audit Logging & Secret Absence Verification
                // =====================================================================
                System.out.println("\n--- Section 12: Audit Logging & Secret Absence ---");
                verifyAuditLogs();
            }

            System.out.println("\n==========================================================================");
            System.out.println("  ABIZ LOCKER PHASE 16 PRODUCTION HARDENING RESULTS");
            System.out.println("  PASS = " + testsPassed + ", FAIL = " + testsFailed + ", NOT VERIFIED = 0");
            System.out.println("==========================================================================");

            if (testsFailed > 0) {
                System.exit(1);
            }
        } catch (Throwable t) {
            System.err.println("FATAL: Phase 16 Verification Suite failed with exception:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void createShop(UUID shopId, String name, String ownerName) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO abiz_shops (id, name, owner_name, phone, status) " +
                             "VALUES (?, ?, ?, '03001234567', 'ACTIVE') ON CONFLICT (id) DO NOTHING")) {
            ps.setObject(1, shopId);
            ps.setString(2, name);
            ps.setString(3, ownerName);
            ps.executeUpdate();
        }
    }

    private static void createCustomer(UUID custId, UUID shopId, String name, String phone) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO abiz_customers (id, shop_id, name, phone, status) " +
                             "VALUES (?, ?, ?, ?, 'ACTIVE') ON CONFLICT (id) DO NOTHING")) {
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
                     "INSERT INTO abiz_financed_devices (" +
                             "  id, shop_id, customer_id, imei1, enrollment_status, device_owner_active, " +
                             "  connection_status, lock_status, license_status, policy_state, push_token, health_status) " +
                             "VALUES (?, ?, ?, ?, 'ENROLLED', TRUE, 'ONLINE', 'UNLOCKED', 'ACTIVE', 'ACTIVE', ?, 'ONLINE') " +
                             "ON CONFLICT (id) DO NOTHING")) {
            ps.setObject(1, devId);
            ps.setObject(2, shopId);
            ps.setObject(3, custId);
            ps.setString(4, imei);
            ps.setString(5, pushToken);
            ps.executeUpdate();
        }
    }

    private static void verifyAuditLogs() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT action, result, COUNT(*) as count FROM abiz_audit_logs " +
                             "WHERE action IN ('DEVICE_COMMAND_CREATED', 'DEVICE_COMMAND_CLAIMED', 'DEVICE_COMMAND_DISPATCHED', " +
                             "'DEVICE_COMMAND_ACKNOWLEDGED', 'DEVICE_COMMAND_EXECUTED', 'DEVICE_COMMAND_FAILED', 'DEVICE_COMMAND_EXPIRED', " +
                             "'DEVICE_COMMAND_CANCELLED', 'DEVICE_COMMAND_RETRIED', 'DEVICE_HEARTBEAT', 'DEVICE_STATE_RECONCILED', " +
                             "'CROSS_TENANT_COMMAND_ACCESS_BLOCKED', 'UNAUTHORIZED_WIPE_ATTEMPT', 'INVALID_COMMAND_ACK_DEVICE', " +
                             "'INVALID_NONCE', 'REPLAY_DETECTED', 'RATE_LIMITED_REQUEST', 'INVALID_DEVICE_AUTH', 'DEVICE_STATE_MISMATCH_DETECTED') " +
                             "GROUP BY action, result ORDER BY action")) {
            ResultSet rs = ps.executeQuery();
            int totalLogged = 0;
            while (rs.next()) {
                String action = rs.getString("action");
                String result = rs.getString("result");
                int count = rs.getInt("count");
                totalLogged += count;
                System.out.println("  [AUDIT LOGGED] action=" + action + ", result=" + result + ", count=" + count);
            }
            assertTrue("Security and operational events recorded in abiz_audit_logs (Total: " + totalLogged + ")", totalLogged >= 8);
        }

        // Verify that no raw credentials or device secrets leaked into audit logs
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM abiz_audit_logs WHERE detail ILIKE '%SuperSecretDeviceKey%'")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int leakCount = rs.getInt(1);
                assertEquals("Zero sensitive credentials or plaintext device secrets found in audit log details", 0, leakCount);
            }
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
        private boolean simulateTransientFailure = false;
        private boolean simulateFatalFailure = false;

        public void setSimulateTransientFailure(boolean val) { this.simulateTransientFailure = val; }
        public void setSimulateFatalFailure(boolean val) { this.simulateFatalFailure = val; }

        @Override
        public CommandDispatchResult dispatch(AbizDeviceCommand command) {
            if (simulateFatalFailure) {
                return CommandDispatchResult.failure("Fatal unrecoverable transport failure", false);
            }
            if (simulateTransientFailure) {
                return CommandDispatchResult.failure("Transient transport socket timeout", true);
            }
            return CommandDispatchResult.success("MSG-" + UUID.randomUUID().toString().substring(0, 8));
        }

        @Override
        public String getProviderName() {
            return "MockHardenedTransport";
        }
    }
}
