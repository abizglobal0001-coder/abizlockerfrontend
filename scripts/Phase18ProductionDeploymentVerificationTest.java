package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.command.*;
import com.abiz.locker.domain.*;
import com.abiz.locker.mapper.*;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Phase 18 Production Deployment, Backup/Restore Verification, Disaster Recovery,
 * Queue Durability & Monitoring Master Verification Suite.
 *
 * Single-file Java 11 executable test suite (120+ distinct PASS/FAIL assertions).
 */
public class Phase18ProductionDeploymentVerificationTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    private static final String DB_USER = "abizlocker";
    private static final String DB_PASS = "abiz_dev_secure_pass_9281";

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static long imeiSequence = 86180000000000L + (System.currentTimeMillis() % 9000000000000L);

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
        System.out.println("  ABIZ LOCKER - PHASE 18 PRODUCTION DEPLOYMENT MASTER VERIFICATION SUITE");
        System.out.println("==========================================================================");

        try {
            PooledDataSource ds = new PooledDataSource("org.postgresql.Driver", DB_URL, DB_USER, DB_PASS);
            Environment env = new Environment("phase18", new JdbcTransactionFactory(), ds);
            Configuration config = new Configuration(env);
            config.getTypeHandlerRegistry().register(UUID.class, new UuidTypeHandler());
            config.addMapper(DeviceCommandMapper.class);
            config.addMapper(FinancedDeviceMapper.class);
            config.addMapper(ShopMapper.class);
            config.addMapper(CustomerMapper.class);
            config.addMapper(AuditMapper.class);
            config.addMapper(SecurityEventMapper.class);
            config.addMapper(BackupRecordMapper.class);
            config.addMapper(DeploymentEventMapper.class);
            config.addMapper(IncidentEventMapper.class);

            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = sqlSessionFactory.openSession(true)) {
                DeviceCommandMapper commandMapper = session.getMapper(DeviceCommandMapper.class);
                FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);
                SecurityEventMapper securityEventMapper = session.getMapper(SecurityEventMapper.class);
                BackupRecordMapper backupRecordMapper = session.getMapper(BackupRecordMapper.class);
                DeploymentEventMapper deploymentMapper = session.getMapper(DeploymentEventMapper.class);
                IncidentEventMapper incidentEventMapper = session.getMapper(IncidentEventMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                java.lang.reflect.Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                f.setAccessible(true);
                f.set(auditService, auditMapper);

                BackupService backupService = new BackupService(backupRecordMapper, ds, auditService);
                BackupVerificationService backupVerifService = new BackupVerificationService(backupRecordMapper, ds, auditService);
                DisasterRecoveryService drService = new DisasterRecoveryService(commandMapper, deviceMapper, ds, auditService);
                DeploymentSafetyService deploySafetyService = new DeploymentSafetyService(deploymentMapper, backupRecordMapper, ds, auditService);
                AlertService alertService = new AlertService(commandMapper, deviceMapper, auditService);
                DataRetentionService retentionService = new DataRetentionService(ds, auditService);
                CommandReconciliationWorker reconWorker = new CommandReconciliationWorker(commandMapper, auditService);
                SecurityEventAggregator securityAggregator = new SecurityEventAggregator(securityEventMapper, incidentEventMapper);

                // Setup test tenant fixtures
                UUID shopA = UUID.randomUUID();
                UUID shopB = UUID.randomUUID();
                createShop(shopA, "P18 Shop A", "Owner P18 A");
                createShop(shopB, "P18 Shop B", "Owner P18 B");

                UUID custA = UUID.randomUUID();
                createCustomer(custA, shopA, "Customer P18 A", "03008881122");
                UUID devA = UUID.randomUUID();
                createDevice(devA, shopA, custA, nextValidImei(), "TOKEN-P18-A");

                UUID devB = UUID.randomUUID();
                createDevice(devB, shopB, null, nextValidImei(), "TOKEN-P18-B");

                runSection1_SchemaAndMigration(ds);
                runSection2_BackupServiceAndChecksum(backupService, backupRecordMapper, ds, shopA);
                runSection3_BackupVerificationAndIsolatedRestore(backupVerifService, backupRecordMapper, ds);
                runSection4_DisasterRecoveryScenarios(drService, commandMapper, deviceMapper, shopA, devA);
                runSection5_DeploymentSafetyChecklist(deploySafetyService, deploymentMapper, shopA);
                runSection6_QueueDurabilityBackoffAndJitter(reconWorker, commandMapper, shopA, devA);
                runSection7_SafeAdministrativeReplay(reconWorker, commandMapper, shopA, devA);
                runSection8_StructuredLoggingAndRedaction(shopA, devA);
                runSection9_SecurityAggregatorAndIncidents(securityAggregator, incidentEventMapper, shopA, devA);
                runSection10_OperationalAlertingAndDeduplication(alertService, commandMapper);
                runSection11_DataRetentionAndBoundedCleanup(retentionService, ds, shopA, devA);
                runSection12_MultiTenantSafetyAndProtocolIntegrity(deviceMapper, commandMapper, shopA, shopB, devA, devB);

                System.out.println("\n==========================================================================");
                System.out.println("  PHASE 18 VERIFICATION SUMMARY: " + testsPassed + " PASS, " + testsFailed + " FAIL");
                System.out.println("  TOTAL: " + testsPassed + " PASS, " + testsFailed + " FAIL");
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
    // Section 1: 18.1 Schema & Migration Verification (10 tests)
    // =====================================================================
    private static void runSection1_SchemaAndMigration(PooledDataSource ds) throws Exception {
        System.out.println("\n--- 18.1: Schema & Migration Verification ---");
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // 1. abiz_backup_records table exists
            try (ResultSet rs = meta.getTables(null, null, "abiz_backup_records", null)) {
                assertTrue("Table abiz_backup_records exists in database", rs.next());
            }

            // 2. abiz_deployment_events table exists
            try (ResultSet rs = meta.getTables(null, null, "abiz_deployment_events", null)) {
                assertTrue("Table abiz_deployment_events exists in database", rs.next());
            }

            // 3. abiz_incident_events table exists
            try (ResultSet rs = meta.getTables(null, null, "abiz_incident_events", null)) {
                assertTrue("Table abiz_incident_events exists in database", rs.next());
            }

            // 4. abiz_device_commands has attempt_count and next_retry_at
            try (ResultSet rs = meta.getColumns(null, null, "abiz_device_commands", "attempt_count")) {
                assertTrue("abiz_device_commands.attempt_count column exists", rs.next());
            }
            try (ResultSet rs = meta.getColumns(null, null, "abiz_device_commands", "next_retry_at")) {
                assertTrue("abiz_device_commands.next_retry_at column exists", rs.next());
            }

            // 5. abiz_financed_devices has integrity_status and agent_protocol_version
            try (ResultSet rs = meta.getColumns(null, null, "abiz_financed_devices", "integrity_status")) {
                assertTrue("abiz_financed_devices.integrity_status column exists", rs.next());
            }
            try (ResultSet rs = meta.getColumns(null, null, "abiz_financed_devices", "agent_protocol_version")) {
                assertTrue("abiz_financed_devices.agent_protocol_version column exists", rs.next());
            }

            // 6. abiz_rate_limit_state table exists
            try (ResultSet rs = meta.getTables(null, null, "abiz_rate_limit_state", null)) {
                assertTrue("Table abiz_rate_limit_state exists in database", rs.next());
            }

            // 7. abiz_backup_records has checksum column
            try (ResultSet rs = meta.getColumns(null, null, "abiz_backup_records", "checksum")) {
                assertTrue("abiz_backup_records.checksum column exists", rs.next());
            }

            // 8. Database is PostgreSQL 14+
            String dbProduct = meta.getDatabaseProductName();
            int majorVersion = meta.getDatabaseMajorVersion();
            assertTrue("Database product is PostgreSQL (found: " + dbProduct + " " + majorVersion + ")",
                    "PostgreSQL".equalsIgnoreCase(dbProduct) && majorVersion >= 14);
        }
    }

    // =====================================================================
    // Section 2: 18.2 Backup Service & SHA-256 Checksum (10 tests)
    // =====================================================================
    private static void runSection2_BackupServiceAndChecksum(BackupService backupService,
                                                             BackupRecordMapper backupRecordMapper,
                                                             PooledDataSource ds,
                                                             UUID shopA) throws Exception {
        System.out.println("\n--- 18.2: Backup Service & SHA-256 Checksum ---");

        // 1. SHA-256 calculation for empty payload
        String emptySha = BackupService.calculateSha256(new byte[0]);
        assertEquals("calculateSha256 for empty byte array matches standard hash",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", emptySha);

        // 2. SHA-256 calculation for known test payload
        byte[] testBytes = "ABIZ_LOCKER_PHASE18_TEST_PAYLOAD".getBytes(StandardCharsets.UTF_8);
        String testSha = BackupService.calculateSha256(testBytes);
        assertNotNull("SHA-256 calculated for test payload", testSha);
        assertEquals("SHA-256 hex string is 64 characters long", 64, testSha.length());

        // 3. Create full database backup
        StructuredEventLogger.setCorrelationId("CORR-BK-001");
        AbizBackupRecord record = backupService.createBackup("abizlocker", "FULL");
        assertNotNull("BackupService.createBackup returns non-null record", record);

        // 4. Backup status is COMPLETED
        assertEquals("Backup record status is COMPLETED", AbizBackupRecord.STATUS_COMPLETED, record.getStatus());

        // 5. Backup size > 0
        assertTrue("Backup sizeBytes is greater than 0 (" + record.getSizeBytes() + " bytes)", record.getSizeBytes() > 0);

        // 6. Checksum is 64 hex chars
        assertNotNull("Backup checksum is non-null", record.getChecksum());
        assertEquals("Backup checksum length is 64 hex chars", 64, record.getChecksum().length());

        // 7. Verify persisted in DB via Mapper
        AbizBackupRecord fromDb = backupRecordMapper.findByBackupId(record.getBackupId());
        assertNotNull("Backup record is queryable from database by backupId", fromDb);
        assertEquals("Persisted backup database identifier matches", "abizlocker", fromDb.getDatabaseIdentifier());

        // 8. Backup correlationId is populated
        assertNotNull("Backup record contains correlationId", fromDb.getCorrelationId());

        // 9. Query recent backups by database
        List<AbizBackupRecord> recent = backupRecordMapper.findRecentByDatabase("abizlocker", 10);
        assertTrue("findRecentByDatabase returns list containing created backup",
                recent.stream().anyMatch(b -> b.getBackupId().equals(record.getBackupId())));

        // 10. Concurrency lock released after completion
        assertFalse("Backup lock is released after backup finishes", backupService.isBackupRunning());
    }

    // =====================================================================
    // Section 3: 18.3 Backup Verification & Isolated Restore (10 tests)
    // =====================================================================
    private static void runSection3_BackupVerificationAndIsolatedRestore(BackupVerificationService backupVerifService,
                                                                         BackupRecordMapper backupRecordMapper,
                                                                         PooledDataSource ds) throws Exception {
        System.out.println("\n--- 18.3: Backup Verification & Isolated Restore ---");

        // 1. Create a mock valid backup record
        AbizBackupRecord record = new AbizBackupRecord();
        record.setId(UUID.randomUUID());
        record.setBackupId("BKP-VERIF-" + System.currentTimeMillis());
        record.setDatabaseIdentifier("abizlocker");
        record.setStatus(AbizBackupRecord.STATUS_COMPLETED);
        byte[] payload = "MOCK_BACKUP_CONTENT_FOR_VERIFICATION".getBytes(StandardCharsets.UTF_8);
        String validChecksum = BackupService.calculateSha256(payload);
        record.setChecksum(validChecksum);
        record.setSizeBytes(payload.length);
        backupRecordMapper.insertBackup(record);

        // 2. Verify with valid payload -> returns true
        boolean verifResult = backupVerifService.verifyBackup(record, payload);
        assertTrue("Backup verification returns true for valid matching checksum and schema probe", verifResult);

        // 3. Database status is VERIFIED / PASSED
        AbizBackupRecord updated = backupRecordMapper.findById(record.getId());
        assertEquals("Backup status updated to VERIFIED", AbizBackupRecord.STATUS_VERIFIED, updated.getStatus());
        assertEquals("Verification status updated to PASSED", AbizBackupRecord.VERIF_PASSED, updated.getVerificationStatus());

        // 4. verified_at timestamp is set
        assertNotNull("Backup verifiedAt timestamp is set", updated.getVerifiedAt());

        // 5. Verify corrupted payload -> returns false
        AbizBackupRecord corruptedRecord = new AbizBackupRecord();
        corruptedRecord.setId(UUID.randomUUID());
        corruptedRecord.setBackupId("BKP-CORRUPT-" + System.currentTimeMillis());
        corruptedRecord.setDatabaseIdentifier("abizlocker");
        corruptedRecord.setStatus(AbizBackupRecord.STATUS_COMPLETED);
        corruptedRecord.setChecksum("deadbeef00000000000000000000000000000000000000000000000000000000");
        corruptedRecord.setSizeBytes(payload.length);
        backupRecordMapper.insertBackup(corruptedRecord);

        boolean corruptResult = backupVerifService.verifyBackup(corruptedRecord, payload);
        assertFalse("Backup verification returns false for checksum mismatch", corruptResult);

        // 6. Corrupted record in DB marked FAILED
        AbizBackupRecord corruptDb = backupRecordMapper.findById(corruptedRecord.getId());
        assertEquals("Corrupted backup verification status is FAILED", AbizBackupRecord.VERIF_FAILED, corruptDb.getVerificationStatus());
        assertNotNull("Corrupted backup failure reason is recorded", corruptDb.getFailureReason());
        assertTrue("Failure reason mentions Checksum mismatch", corruptDb.getFailureReason().contains("Checksum mismatch"));

        // 7. Verify null record handling
        boolean threwNpe = false;
        try {
            backupVerifService.verifyBackup(null, payload);
        } catch (IllegalArgumentException e) {
            threwNpe = true;
        }
        assertTrue("verifyBackup(null) throws IllegalArgumentException", threwNpe);

        // 8. Isolated restore tables cleaned up
        try (Connection conn = ds.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "abiz_restore_test_%", null)) {
                assertFalse("Isolated restore temporary tables dropped cleanly without lingering", rs.next());
            }
        }

        // 9. Count successful backups
        long successfulBackups = backupRecordMapper.countSuccessfulBackups();
        assertTrue("countSuccessfulBackups returns >= 1", successfulBackups >= 1);

        // 10. countAll backups
        long allBackups = backupRecordMapper.countAll();
        assertTrue("countAll backups returns >= 2", allBackups >= 2);
    }

    // =====================================================================
    // Section 4: 18.4 Disaster Recovery Scenarios (10 tests)
    // =====================================================================
    private static void runSection4_DisasterRecoveryScenarios(DisasterRecoveryService drService,
                                                             DeviceCommandMapper commandMapper,
                                                             FinancedDeviceMapper deviceMapper,
                                                             UUID shopA,
                                                             UUID devA) throws Exception {
        System.out.println("\n--- 18.4: Disaster Recovery Scenarios ---");

        // 1. Scenario A: Force outage simulation -> returns false (fail-closed)
        boolean outageHandling = drService.simulateDatabaseOutageHandling(true);
        assertFalse("Scenario A: simulateDatabaseOutageHandling(true) enforces fail-closed and returns false", outageHandling);

        // 2. Scenario A: Normal database connection check -> returns true
        boolean normalHandling = drService.simulateDatabaseOutageHandling(false);
        assertTrue("Scenario A: simulateDatabaseOutageHandling(false) returns true when database is healthy", normalHandling);

        // 3. Scenario B: Worker crash recovery with stuck DISPATCHING command
        UUID stuckCmdId = UUID.randomUUID();
        createRawCommandWithDates(stuckCmdId, shopA, devA, "LOCK", "DISPATCHING",
                new Date(System.currentTimeMillis() - 7200000L), new Date(System.currentTimeMillis() + 3600000L));

        int recovered = drService.recoverWorkerCrash(1000L);
        assertTrue("Scenario B: recoverWorkerCrash identifies and recovers stuck dispatching command (recovered=" + recovered + ")", recovered >= 1);

        // 4. Stuck command reset to PENDING
        AbizDeviceCommand recoveredCmd = commandMapper.findCommandById(stuckCmdId);
        assertEquals("Scenario B: Stuck command status transitioned back to PENDING", AbizDeviceCommand.STATUS_PENDING, recoveredCmd.getStatus());

        // 5. Scenario B: Fresh dispatching command is NOT reset
        UUID freshCmdId = UUID.randomUUID();
        createRawCommandWithDates(freshCmdId, shopA, devA, "LOCK", "DISPATCHING",
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis() + 3600000L));
        int freshRecovered = drService.recoverWorkerCrash(3600000L); // 1 hour lease
        AbizDeviceCommand freshCmd = commandMapper.findCommandById(freshCmdId);
        assertEquals("Scenario B: Unexpired dispatching command remains in DISPATCHING status", AbizDeviceCommand.STATUS_DISPATCHING, freshCmd.getStatus());

        // 6. Scenario C: ACK Loss Reconciliation for LOCK command
        UUID ackLossCmdId = UUID.randomUUID();
        createRawCommand(ackLossCmdId, shopA, devA, "LOCK", "SENT", new Date(System.currentTimeMillis() + 3600000L));
        boolean reconciled = drService.reconcileAckLoss(ackLossCmdId, "LOCKED");
        assertTrue("Scenario C: reconcileAckLoss returns true when physical state matches LOCK command", reconciled);

        // 7. Command status marked EXECUTED and RECONCILED
        AbizDeviceCommand reconCmd = commandMapper.findCommandById(ackLossCmdId);
        assertEquals("Scenario C: Reconciled command status is EXECUTED", AbizDeviceCommand.STATUS_EXECUTED, reconCmd.getStatus());
        assertEquals("Scenario C: Reconciled command state is RECONCILED", "RECONCILED", reconCmd.getReconciliationState());

        // 8. Device last_successful_command_at is updated
        AbizFinancedDevice devRecord = deviceMapper.findDeviceById(devA);
        assertNotNull("Scenario C: Device last_successful_command_at updated on reconciliation", devRecord.getLastSuccessfulCommandAt());

        // 9. Scenario C: Mismatched state returns false
        UUID mismatchCmdId = UUID.randomUUID();
        createRawCommand(mismatchCmdId, shopA, devA, "LOCK", "SENT", new Date(System.currentTimeMillis() + 3600000L));
        boolean mismatchResult = drService.reconcileAckLoss(mismatchCmdId, "UNLOCKED");
        assertFalse("Scenario C: reconcileAckLoss returns false on physical state mismatch", mismatchResult);

        // 10. Scenario D: Database restore compatibility validation
        boolean schemaCompat = drService.validateRestoredDatabaseCompatibility();
        assertTrue("Scenario D: validateRestoredDatabaseCompatibility() returns true for valid production schema", schemaCompat);
    }

    // =====================================================================
    // Section 5: 18.5 Deployment Safety & 10-Point Checklist (10 tests)
    // =====================================================================
    private static void runSection5_DeploymentSafetyChecklist(DeploymentSafetyService deploySafetyService,
                                                               DeploymentEventMapper deploymentMapper,
                                                               UUID shopA) {
        System.out.println("\n--- 18.5: Deployment Safety & 10-Point Checklist ---");

        // 1. Evaluate 10-point checklist
        Map<String, Boolean> checklist = deploySafetyService.evaluatePreflightChecklist("production", true, true);
        assertNotNull("Preflight checklist map is non-null", checklist);
        assertEquals("Preflight checklist contains exactly 10 checks", 10, checklist.size());

        // 2. All 10 checks pass when backupVerified=true, androidCompatible=true
        boolean passed = deploySafetyService.isPreflightPassed(checklist);
        assertTrue("isPreflightPassed returns true when all 10 checks pass", passed);

        // 3. Preflight fails when backup is not verified
        Map<String, Boolean> unverifiedChecklist = deploySafetyService.evaluatePreflightChecklist("production", false, true);
        assertFalse("isPreflightPassed returns false when backup is unverified", deploySafetyService.isPreflightPassed(unverifiedChecklist));

        // 4. Preflight fails when Android protocol is incompatible
        Map<String, Boolean> incompatibleChecklist = deploySafetyService.evaluatePreflightChecklist("production", true, false);
        assertFalse("isPreflightPassed returns false when Android protocol is incompatible", deploySafetyService.isPreflightPassed(incompatibleChecklist));

        // 5. Start deployment
        String depId = "DEP-P18-" + System.currentTimeMillis();
        AbizDeploymentEvent event = deploySafetyService.startDeployment(depId, "0.1.18", "production", shopA);
        assertNotNull("startDeployment returns deployment event", event);
        assertEquals("Initial deployment status is PRECHECK", AbizDeploymentEvent.STATUS_PRECHECK, event.getStatus());

        // 6. Transition PRECHECK -> APPROVED
        boolean t1 = deploySafetyService.transitionStatus(event, AbizDeploymentEvent.STATUS_APPROVED, "Checklist verified");
        assertTrue("Transition PRECHECK -> APPROVED succeeded", t1);
        assertEquals("Deployment status is APPROVED", AbizDeploymentEvent.STATUS_APPROVED, event.getStatus());

        // 7. Transition APPROVED -> DEPLOYING
        boolean t2 = deploySafetyService.transitionStatus(event, AbizDeploymentEvent.STATUS_DEPLOYING, "Deploying WAR");
        assertTrue("Transition APPROVED -> DEPLOYING succeeded", t2);
        assertEquals("Deployment status is DEPLOYING", AbizDeploymentEvent.STATUS_DEPLOYING, event.getStatus());

        // 8. Transition DEPLOYING -> VERIFYING
        boolean t3 = deploySafetyService.transitionStatus(event, AbizDeploymentEvent.STATUS_VERIFYING, "Verifying smoke tests");
        assertTrue("Transition DEPLOYING -> VERIFYING succeeded", t3);

        // 9. Rejects illegal transition (e.g. PRECHECK directly to COMPLETED)
        AbizDeploymentEvent freshDep = deploySafetyService.startDeployment("DEP-ILLEGAL-" + System.currentTimeMillis(), "0.1.18", "production", shopA);
        boolean illegal = deploySafetyService.transitionStatus(freshDep, AbizDeploymentEvent.STATUS_COMPLETED, "Illegal skip");
        assertFalse("Illegal transition PRECHECK -> COMPLETED rejected", illegal);

        // 10. Execute safe rollback
        boolean rollbackOk = deploySafetyService.executeSafeRollback(event, "Smoke test failed in verification stage");
        assertTrue("executeSafeRollback transitions deployment to ROLLED_BACK", rollbackOk);
        assertEquals("Deployment status is ROLLED_BACK", AbizDeploymentEvent.STATUS_ROLLED_BACK, event.getStatus());
        assertNotNull("Rollback timestamp recorded", event.getRollbackAt());
    }

    // =====================================================================
    // Section 6: 18.6 Queue Durability, Exponential Backoff & Jitter (10 tests)
    // =====================================================================
    private static void runSection6_QueueDurabilityBackoffAndJitter(CommandReconciliationWorker reconWorker,
                                                                   DeviceCommandMapper commandMapper,
                                                                   UUID shopA,
                                                                   UUID devA) throws Exception {
        System.out.println("\n--- 18.6: Queue Durability, Exponential Backoff & Jitter ---");

        // 1. Backoff delay calculation - attempt 0
        long d0 = CommandReconciliationWorker.calculateRetryDelay(0, 2000L, 120000L, 0L);
        assertEquals("Attempt 0 backoff delay is base delay (2000ms)", 2000L, d0);

        // 2. Backoff delay calculation - attempt 1 (2^1 * 2000 = 4000ms)
        long d1 = CommandReconciliationWorker.calculateRetryDelay(1, 2000L, 120000L, 0L);
        assertEquals("Attempt 1 backoff delay is 4000ms", 4000L, d1);

        // 3. Backoff delay calculation - attempt 2 (2^2 * 2000 = 8000ms)
        long d2 = CommandReconciliationWorker.calculateRetryDelay(2, 2000L, 120000L, 0L);
        assertEquals("Attempt 2 backoff delay is 8000ms", 8000L, d2);

        // 4. Backoff delay calculation - attempt 3 (2^3 * 2000 = 16000ms)
        long d3 = CommandReconciliationWorker.calculateRetryDelay(3, 2000L, 120000L, 0L);
        assertEquals("Attempt 3 backoff delay is 16000ms", 16000L, d3);

        // 5. Backoff delay calculation - max delay cap
        long dMax = CommandReconciliationWorker.calculateRetryDelay(10, 2000L, 60000L, 0L);
        assertEquals("Attempt 10 delay is capped at maxDelay (60000ms)", 60000L, dMax);

        // 6. Bounded jitter within [delay - jitter, delay + jitter]
        long dJitter = CommandReconciliationWorker.calculateRetryDelay(2, 2000L, 120000L, 500L);
        assertTrue("Delay with jitter is between 7500ms and 8500ms (Got: " + dJitter + ")", dJitter >= 7500L && dJitter <= 8500L);

        // 7. Process command failure (attempt 1) -> schedules next retry
        UUID cmdId = UUID.randomUUID();
        createRawCommand(cmdId, shopA, devA, "LOCK", "PENDING", new Date(System.currentTimeMillis() + 3600000L));
        boolean scheduled = reconWorker.processCommandFailure(cmdId, "NETWORK_TIMEOUT", "Connection dropped");
        assertTrue("processCommandFailure schedules retry for attempt 1", scheduled);

        // 8. Command in DB has updated attempt_count and next_retry_at
        AbizDeviceCommand cmdDb = commandMapper.findCommandById(cmdId);
        assertEquals("Command attempt_count incremented to 1", 1, cmdDb.getAttemptCount());
        assertEquals("Command last_error_code is NETWORK_TIMEOUT", "NETWORK_TIMEOUT", cmdDb.getLastErrorCode());
        assertNotNull("Command next_retry_at is scheduled", cmdDb.getNextRetryAt());

        // 9. Process failure when max attempts reached -> dead-letters command
        cmdDb.setAttemptCount(4);
        cmdDb.setMaxAttempts(5);
        commandMapper.updateCommand(cmdDb);
        boolean deadLettered = reconWorker.processCommandFailure(cmdId, "AUTH_FAILED", "Persistent key rejection");
        assertFalse("processCommandFailure returns false when max attempts exhausted (dead-lettered)", deadLettered);

        // 10. Mark command completed
        UUID completeCmdId = UUID.randomUUID();
        createRawCommand(completeCmdId, shopA, devA, "UNLOCK", "PENDING", new Date(System.currentTimeMillis() + 3600000L));
        commandMapper.markCommandCompleted(completeCmdId, new Date());
        AbizDeviceCommand completedDb = commandMapper.findCommandById(completeCmdId);
        assertEquals("markCommandCompleted sets status to EXECUTED", AbizDeviceCommand.STATUS_EXECUTED, completedDb.getStatus());
        assertEquals("markCommandCompleted sets reconciliation_state to RECONCILED", "RECONCILED", completedDb.getReconciliationState());
    }

    // =====================================================================
    // Section 7: 18.7 Safe Administrative Replay (10 tests)
    // =====================================================================
    private static void runSection7_SafeAdministrativeReplay(CommandReconciliationWorker reconWorker,
                                                             DeviceCommandMapper commandMapper,
                                                             UUID shopA,
                                                             UUID devA) throws Exception {
        System.out.println("\n--- 18.7: Safe Administrative Replay ---");

        // 1. Setup dead-lettered command in DB
        UUID dlCmdId = UUID.randomUUID();
        createRawCommand(dlCmdId, shopA, devA, "LOCK", "FAILED", new Date(System.currentTimeMillis() + 3600000L));
        commandMapper.recordDeadLetterWithReason(dlCmdId, "MAX_RETRIES_EXCEEDED: Timeout", "TIMEOUT_EXHAUSTED", new Date());

        AbizDeviceCommand dlCmd = commandMapper.findCommandById(dlCmdId);
        assertEquals("Command status is FAILED", AbizDeviceCommand.STATUS_FAILED, dlCmd.getStatus());
        assertEquals("Reconciliation state is DEAD_LETTERED", "DEAD_LETTERED", dlCmd.getReconciliationState());
        assertNotNull("Dead letter reason recorded", dlCmd.getDeadLetterReason());

        // 2. Trigger safe replay with fresh TTL
        String originalNonce = dlCmd.getNonce();
        String originalRequestId = dlCmd.getRequestId();
        UUID actorId = UUID.randomUUID();

        boolean replayResult = reconWorker.safeReplayFromDeadLetter(dlCmdId, 30, actorId);
        assertTrue("safeReplayFromDeadLetter returns true", replayResult);

        // 3. Replayed command status reset to PENDING
        AbizDeviceCommand replayedCmd = commandMapper.findCommandById(dlCmdId);
        assertEquals("Replayed command status reset to PENDING", AbizDeviceCommand.STATUS_PENDING, replayedCmd.getStatus());

        // 4. Reconciliation state is REPLAYED
        assertEquals("Replayed command reconciliation state is REPLAYED", "REPLAYED", replayedCmd.getReconciliationState());

        // 5. Attempt count reset to 0
        assertEquals("Replayed command attempt_count reset to 0", 0, replayedCmd.getAttemptCount());
        assertEquals("Replayed command retry_count reset to 0", 0, replayedCmd.getRetryCount());

        // 6. Dead letter metadata cleared
        assertEquals("Dead letter reason cleared", null, replayedCmd.getDeadLetterReason());
        assertEquals("Dead letter timestamp cleared", null, replayedCmd.getDeadLetteredAt());
        assertEquals("Last error code cleared", null, replayedCmd.getLastErrorCode());

        // 7. Fresh security nonce generated (not equal to original)
        assertNotNull("Fresh nonce is non-null", replayedCmd.getNonce());
        assertTrue("Fresh nonce is 32 characters long", replayedCmd.getNonce().length() == 32);
        assertTrue("Fresh nonce differs from original nonce", !replayedCmd.getNonce().equals(originalNonce));

        // 8. Fresh request ID generated (starts with REQ-)
        assertNotNull("Fresh requestId is non-null", replayedCmd.getRequestId());
        assertTrue("Fresh requestId starts with REQ-", replayedCmd.getRequestId().startsWith("REQ-"));
        assertTrue("Fresh requestId differs from original requestId", !replayedCmd.getRequestId().equals(originalRequestId));

        // 9. Fresh idempotency key generated
        assertNotNull("Fresh idempotencyKey generated", replayedCmd.getIdempotencyKey());

        // 10. Fresh expiration timestamp set in the future
        assertTrue("Fresh expires_at is set in the future", replayedCmd.getExpiresAt().after(new Date()));
    }

    // =====================================================================
    // Section 8: 18.8 Structured Logging & Secret Redaction (10 tests)
    // =====================================================================
    private static void runSection8_StructuredLoggingAndRedaction(UUID shopA, UUID devA) {
        System.out.println("\n--- 18.8: Structured Logging & Secret Redaction ---");

        // 1. Redact long secret (16 chars)
        String r1 = StructuredEventLogger.redactSecret("abcdef1234567890");
        assertEquals("redactSecret keeps first 4 and last 4 chars", "abcd****7890", r1);

        // 2. Redact short secret (<= 8 chars)
        String r2 = StructuredEventLogger.redactSecret("short12");
        assertEquals("redactSecret returns **** for short strings", "****", r2);

        // 3. Redact null secret
        String r3 = StructuredEventLogger.redactSecret(null);
        assertEquals("redactSecret returns null for null input", null, r3);

        // 4. Redact message containing password=...
        String m1 = StructuredEventLogger.redactMessage("Connecting to DB with password=supersecretpass to host");
        assertTrue("redactMessage redacts password value", m1.contains("password=****") && !m1.contains("supersecretpass"));

        // 5. Redact message containing token=...
        String m2 = StructuredEventLogger.redactMessage("Auth token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9 for user");
        assertTrue("redactMessage redacts token value", m2.contains("token=****") && !m2.contains("eyJhbG"));

        // 6. Redact message containing apiKey=...
        String m3 = StructuredEventLogger.redactMessage("Using apiKey=API-KEY-99881122 for cloud dispatch");
        assertTrue("redactMessage redacts apiKey value", m3.contains("apiKey=****") && !m3.contains("API-KEY-99881122"));

        // 7. ThreadLocal context management: set and get
        StructuredEventLogger.setRequestId("REQ-P18-TST");
        StructuredEventLogger.setCorrelationId("CORR-P18-TST");
        StructuredEventLogger.setActorId(shopA);
        assertEquals("getRequestId retrieves set requestId", "REQ-P18-TST", StructuredEventLogger.getRequestId());
        assertEquals("getCorrelationId retrieves set correlationId", "CORR-P18-TST", StructuredEventLogger.getCorrelationId());
        assertEquals("getActorId retrieves set actorId", shopA, StructuredEventLogger.getActorId());

        // 8. Clear context clears all ThreadLocal variables
        StructuredEventLogger.clearContext();
        assertEquals("clearContext clears requestId", null, StructuredEventLogger.getRequestId());
        assertEquals("clearContext clears correlationId", null, StructuredEventLogger.getCorrelationId());
        assertEquals("clearContext clears actorId", null, StructuredEventLogger.getActorId());

        // 9. Structured security logging does not throw exception
        boolean secLogOk = true;
        try {
            StructuredEventLogger.security("UNAUTHORIZED_WIPE", shopA, devA, "Blocked wipe attempt");
        } catch (Exception e) {
            secLogOk = false;
        }
        assertTrue("StructuredEventLogger.security executes cleanly", secLogOk);

        // 10. Long message (>500 chars) in security logger is truncated
        String longMsg = new String(new char[600]).replace('\0', 'A');
        boolean truncOk = true;
        try {
            StructuredEventLogger.security("SUSPICIOUS_OVERFLOW", shopA, devA, longMsg);
        } catch (Exception e) {
            truncOk = false;
        }
        assertTrue("StructuredEventLogger.security handles long strings safely", truncOk);
    }

    // =====================================================================
    // Section 9: 18.9 Security Event Aggregator & Incident Escalation (10 tests)
    // =====================================================================
    private static void runSection9_SecurityAggregatorAndIncidents(SecurityEventAggregator securityAggregator,
                                                                  IncidentEventMapper incidentEventMapper,
                                                                  UUID shopA,
                                                                  UUID devA) {
        System.out.println("\n--- 18.9: Security Event Aggregator & Incident Escalation ---");

        // 1. Reset counters
        securityAggregator.resetCounters();
        assertEquals("Initial event count is 0", 0L, securityAggregator.getEventCount(devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH));

        // 2. Record single event
        securityAggregator.recordEvent(shopA, devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH, "Rate limit hit", "192.168.1.100");
        assertEquals("Event count incremented to 1", 1L, securityAggregator.getEventCount(devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH));

        // 3. Record 4 more events to reach threshold (5) -> triggers incident creation
        securityAggregator.recordEvent(shopA, devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH, "Burst 2", "192.168.1.100");
        securityAggregator.recordEvent(shopA, devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH, "Burst 3", "192.168.1.100");
        securityAggregator.recordEvent(shopA, devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH, "Burst 4", "192.168.1.100");
        securityAggregator.recordEvent(shopA, devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH, "Burst 5", "192.168.1.100");
        assertEquals("Event count is 5", 5L, securityAggregator.getEventCount(devA, SecurityEventAggregator.EVENT_RATE_LIMIT_BREACH));

        // 4. Incident event created in DB with status OPEN and severity HIGH
        List<AbizIncidentEvent> openIncidents = incidentEventMapper.findOpenIncidents(10);
        assertTrue("findOpenIncidents returns non-empty list after threshold reached", !openIncidents.isEmpty());
        AbizIncidentEvent createdIncident = openIncidents.get(0);
        assertEquals("Escalated incident severity is HIGH", AbizIncidentEvent.SEVERITY_HIGH, createdIncident.getSeverity());
        assertEquals("Escalated incident status is OPEN", AbizIncidentEvent.STATUS_OPEN, createdIncident.getStatus());

        // 5. Query incidents by shop
        List<AbizIncidentEvent> shopIncidents = incidentEventMapper.findRecentByShop(shopA, 10);
        assertTrue("findRecentByShop returns incidents for shopA", !shopIncidents.isEmpty());

        // 6. Update incident status to RESOLVED
        int updatedRows = incidentEventMapper.updateIncidentStatus(createdIncident.getId(), AbizIncidentEvent.STATUS_RESOLVED, new Date(), "Rate limit investigated and cleared");
        assertEquals("updateIncidentStatus updated 1 row", 1, updatedRows);

        // 7. Verify updated incident in DB
        AbizIncidentEvent resolvedDb = incidentEventMapper.findById(createdIncident.getId());
        assertEquals("Incident status is RESOLVED", AbizIncidentEvent.STATUS_RESOLVED, resolvedDb.getStatus());
        assertNotNull("Incident resolved_at timestamp is populated", resolvedDb.getResolvedAt());
        assertNotNull("Incident resolution_notes is populated", resolvedDb.getResolutionNotes());

        // 8. Count open incidents
        long openCount = incidentEventMapper.countOpenIncidents();
        assertTrue("countOpenIncidents is non-negative", openCount >= 0);

        // 9. Count all incidents
        long allCount = incidentEventMapper.countAll();
        assertTrue("countAll incidents returns >= 1", allCount >= 1);

        // 10. Gracefully handles empty eventType
        boolean emptyHandled = true;
        try {
            securityAggregator.recordEvent(shopA, devA, "", "Empty type");
        } catch (Exception e) {
            emptyHandled = false;
        }
        assertTrue("recordEvent handles empty eventType without error", emptyHandled);
    }

    // =====================================================================
    // Section 10: 18.10 Operational Alerting & Deduplication (10 tests)
    // =====================================================================
    private static void runSection10_OperationalAlertingAndDeduplication(AlertService alertService,
                                                                        DeviceCommandMapper commandMapper) {
        System.out.println("\n--- 18.10: Operational Alerting & Deduplication ---");

        // 1. Clear active alerts
        alertService.clearAlerts();
        assertEquals("Initial active alert count is 0", 0, alertService.getActiveAlertCount());

        // 2. Raise new alert -> returns true (first time dispatched)
        boolean a1 = alertService.raiseAlert("QUEUE_DEPTH_EXCEEDED", "HIGH", "Command queue depth exceeds threshold");
        assertTrue("Initial raiseAlert returns true (dispatched)", a1);

        // 3. Immediate duplicate alert -> returns false (suppressed / deduplicated)
        boolean a2 = alertService.raiseAlert("QUEUE_DEPTH_EXCEEDED", "HIGH", "Command queue depth exceeds threshold");
        assertFalse("Duplicate raiseAlert within window returns false (suppressed)", a2);

        // 4. Deduplicated alert occurrenceCount incremented to 2
        AlertService.AlertEntry entry = alertService.getAlert("QUEUE_DEPTH_EXCEEDED");
        assertNotNull("AlertEntry is non-null", entry);
        assertEquals("Alert occurrenceCount is 2", 2, entry.occurrenceCount);
        assertEquals("Alert severity is HIGH", "HIGH", entry.severity);

        // 5. Raise distinct alert key -> returns true (new alert dispatched)
        boolean a3 = alertService.raiseAlert("DISK_USAGE_CRITICAL", "CRITICAL", "Storage usage at 96%");
        assertTrue("Distinct alert key returns true (dispatched)", a3);

        // 6. Active alert count is 2
        assertEquals("Active alert count is 2", 2, alertService.getActiveAlertCount());

        // 7. Evaluate operational thresholds runs cleanly
        boolean evalOk = true;
        try {
            alertService.evaluateOperationalThresholds(500, 100);
        } catch (Exception e) {
            evalOk = false;
        }
        assertTrue("evaluateOperationalThresholds executes without error", evalOk);

        // 8. Null alert key handled safely
        boolean nullAlertResult = alertService.raiseAlert(null, "HIGH", "Null key test");
        assertFalse("raiseAlert(null) returns false safely", nullAlertResult);

        // 9. Clear alerts empties active map
        alertService.clearAlerts();
        assertEquals("getActiveAlertCount is 0 after clearAlerts", 0, alertService.getActiveAlertCount());
        assertEquals("getAlert returns null for cleared alert", null, alertService.getAlert("QUEUE_DEPTH_EXCEEDED"));

        // 10. Alert can be raised again after clear
        boolean a4 = alertService.raiseAlert("QUEUE_DEPTH_EXCEEDED", "HIGH", "Re-dispatched alert");
        assertTrue("raiseAlert returns true after clearing prior alerts", a4);
    }

    // =====================================================================
    // Section 11: 18.11 Data Retention & Bounded Cleanup (10 tests)
    // =====================================================================
    private static void runSection11_DataRetentionAndBoundedCleanup(DataRetentionService retentionService,
                                                                   PooledDataSource ds,
                                                                   UUID shopA,
                                                                   UUID devA) throws Exception {
        System.out.println("\n--- 18.11: Data Retention & Bounded Cleanup ---");

        Date oldDate = new Date(System.currentTimeMillis() - 40L * 24 * 3600 * 1000L); // 40 days ago
        Date cutoff = new Date(System.currentTimeMillis() - 30L * 24 * 3600 * 1000L);  // 30 days ago

        // 1. Create old CANCELLED command
        UUID cancelledId = UUID.randomUUID();
        createRawCommandWithDates(cancelledId, shopA, devA, "LOCK", "CANCELLED", oldDate, oldDate);

        // 2. Create old EXPIRED command
        UUID expiredId = UUID.randomUUID();
        createRawCommandWithDates(expiredId, shopA, devA, "LOCK", "EXPIRED", oldDate, oldDate);

        // 3. Create old PENDING command (MUST NOT be deleted)
        UUID pendingId = UUID.randomUUID();
        createRawCommandWithDates(pendingId, shopA, devA, "LOCK", "PENDING", oldDate, new Date(System.currentTimeMillis() + 3600000L));

        // 4. Create old EXECUTED command (MUST NOT be deleted)
        UUID executedId = UUID.randomUUID();
        createRawCommandWithDates(executedId, shopA, devA, "LOCK", "EXECUTED", oldDate, oldDate);

        // 5. Run cleanup of expired/cancelled commands
        int deletedCommands = retentionService.cleanupExpiredCommands(cutoff, 100);
        assertTrue("cleanupExpiredCommands deleted at least 2 terminal expired/cancelled commands (deleted=" + deletedCommands + ")", deletedCommands >= 2);

        // 6. Verify active PENDING command was NOT deleted
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(1) AS cnt FROM abiz_device_commands WHERE id = ?")) {
            ps.setObject(1, pendingId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("Active PENDING command was NOT deleted by retention service", rs.next() && rs.getInt("cnt") == 1);
            }
        }

        // 7. Verify active EXECUTED command was NOT deleted
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(1) AS cnt FROM abiz_device_commands WHERE id = ?")) {
            ps.setObject(1, executedId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue("Active EXECUTED command was NOT deleted by retention service", rs.next() && rs.getInt("cnt") == 1);
            }
        }

        // 8. Insert transient rate limit state older than 2 hours
        Date oldRateLimitDate = new Date(System.currentTimeMillis() - 7200000L);
        insertRateLimitState("DEV-RATE-" + UUID.randomUUID().toString().substring(0, 8), "DEVICE", "COMMAND_DISPATCH", 10, oldRateLimitDate);

        // 9. Run rate limit state retention cleanup (cutoff = 1 hour ago)
        Date rateLimitCutoff = new Date(System.currentTimeMillis() - 3600000L);
        int deletedRateLimit = retentionService.cleanupOldRateLimitState(rateLimitCutoff, 100);
        assertTrue("cleanupOldRateLimitState deleted expired rate limit records (deleted=" + deletedRateLimit + ")", deletedRateLimit >= 1);

        // 10. Bounded cleanup with empty batch size returns 0 safely
        int zeroCleanup = retentionService.cleanupExpiredCommands(cutoff, 0);
        assertEquals("cleanupExpiredCommands with limit 0 returns 0", 0, zeroCleanup);
    }

    // =====================================================================
    // Section 12: 18.12 Multi-Tenant Safety & Protocol Integrity (10 tests)
    // =====================================================================
    private static void runSection12_MultiTenantSafetyAndProtocolIntegrity(FinancedDeviceMapper deviceMapper,
                                                                           DeviceCommandMapper commandMapper,
                                                                           UUID shopA,
                                                                           UUID shopB,
                                                                           UUID devA,
                                                                           UUID devB) throws Exception {
        System.out.println("\n--- 18.12: Multi-Tenant Safety & Protocol Integrity ---");

        // 1. Update integrity status on device A
        int u1 = deviceMapper.updateIntegrityStatus(devA, "VERIFIED");
        assertEquals("updateIntegrityStatus updated 1 row", 1, u1);

        // 2. Count devices by integrity status
        long verifiedCount = deviceMapper.countDevicesByIntegrity("VERIFIED");
        assertTrue("countDevicesByIntegrity(VERIFIED) returns >= 1", verifiedCount >= 1);

        // 3. Update agent protocol version on device A
        int u2 = deviceMapper.updateAgentProtocolVersion(devA, "2.0");
        assertEquals("updateAgentProtocolVersion updated 1 row", 1, u2);

        // 4. Verify agent protocol version in DB
        AbizFinancedDevice devADb = deviceMapper.findDeviceById(devA);
        assertEquals("Device A agent_protocol_version is 2.0", "2.0", devADb.getAgentProtocolVersion());
        assertEquals("Device A integrity_status is VERIFIED", "VERIFIED", devADb.getIntegrityStatus());

        // 5. Update last successful command execution timestamp
        Date cmdTime = new Date();
        int u3 = deviceMapper.updateLastSuccessfulCommand(devA, cmdTime);
        assertEquals("updateLastSuccessfulCommand updated 1 row", 1, u3);

        // 6. Verify last_successful_command_at in DB
        AbizFinancedDevice devADb2 = deviceMapper.findDeviceById(devA);
        assertNotNull("Device A last_successful_command_at is non-null", devADb2.getLastSuccessfulCommandAt());

        // 7. Multi-Tenant Device Isolation: Shop A device query contains ZERO Shop B devices
        List<AbizFinancedDevice> shopADevices = deviceMapper.findDevicesByShop(shopA, null, null, null, null, null, null, 100, 0);
        boolean leakedDevB = shopADevices.stream().anyMatch(d -> d.getShopId().equals(shopB));
        assertFalse("Shop A device list contains ZERO Shop B devices", leakedDevB);

        // 8. Multi-Tenant Command Isolation: Shop A command query contains ZERO Shop B commands
        UUID cmdShopB = UUID.randomUUID();
        createRawCommand(cmdShopB, shopB, devB, "LOCK", "PENDING", new Date(System.currentTimeMillis() + 3600000L));

        List<AbizDeviceCommand> shopACommands = commandMapper.findCommandsByShop(shopA, 100, 0);
        boolean leakedCmdB = shopACommands.stream().anyMatch(c -> c.getShopId().equals(shopB));
        assertFalse("Shop A commands query contains ZERO Shop B commands", leakedCmdB);

        // 9. Command protocol version stored properly
        AbizDeviceCommand shopBCmd = commandMapper.findCommandById(cmdShopB);
        assertEquals("Shop B command has protocolVersion 1", 1, shopBCmd.getProtocolVersion());

        // 10. Cross-tenant verification: Shop B command belongs to Shop B
        assertEquals("Shop B command shop_id matches Shop B ID", shopB, shopBCmd.getShopId());
    }

    // =====================================================================
    // Test Database Fixtures & Helper Methods
    // =====================================================================

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
                             "push_token, health_status, integrity_status, agent_protocol_version, last_heartbeat_at, created_at, updated_at) " +
                             "VALUES (?, ?, ?, ?, 'ENROLLED', TRUE, 'ONLINE', 'UNLOCKED', 'ACTIVE', 'ACTIVE', " +
                             "?, 'UNKNOWN', 'VERIFIED', 'v1', NOW(), NOW(), NOW()) ON CONFLICT (id) DO NOTHING")) {
            ps.setObject(1, devId);
            ps.setObject(2, shopId);
            ps.setObject(3, custId);
            ps.setString(4, imei);
            ps.setString(5, pushToken);
            ps.executeUpdate();
        }
    }

    private static void createRawCommand(UUID id, UUID shopId, UUID devId, String type, String status, Date expiresAt) throws Exception {
        createRawCommandWithDates(id, shopId, devId, type, status, new Date(), expiresAt);
    }

    private static void createRawCommandWithDates(UUID id, UUID shopId, UUID devId, String type, String status, Date createdAt, Date expiresAt) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO abiz_device_commands (id, shop_id, financed_device_id, command_type, status, " +
                             "idempotency_key, correlation_id, retry_count, max_retries, nonce, request_id, protocol_version, " +
                             "attempt_count, max_attempts, created_at, updated_at, expires_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 3, '12345678901234567890123456789012', 'REQ-00000000', 1, " +
                             "0, 5, ?, ?, ?)")) {
            ps.setObject(1, id);
            ps.setObject(2, shopId);
            ps.setObject(3, devId);
            ps.setString(4, type);
            ps.setString(5, status);
            ps.setString(6, "IDEMP-" + UUID.randomUUID().toString().substring(0, 8));
            ps.setString(7, UUID.randomUUID().toString());
            ps.setTimestamp(8, new java.sql.Timestamp(createdAt.getTime()));
            ps.setTimestamp(9, new java.sql.Timestamp(createdAt.getTime()));
            ps.setTimestamp(10, new java.sql.Timestamp(expiresAt.getTime()));
            ps.executeUpdate();
        }
    }

    private static void insertRateLimitState(String entityId, String entityType, String eventType, int count, Date lastUpdated) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO abiz_rate_limit_state (entity_id, entity_type, event_type, event_count, window_start, last_updated) " +
                             "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (entity_id, entity_type, event_type) DO UPDATE SET last_updated = ?")) {
            ps.setString(1, entityId);
            ps.setString(2, entityType);
            ps.setString(3, eventType);
            ps.setInt(4, count);
            ps.setTimestamp(5, new java.sql.Timestamp(lastUpdated.getTime()));
            ps.setTimestamp(6, new java.sql.Timestamp(lastUpdated.getTime()));
            ps.setTimestamp(7, new java.sql.Timestamp(lastUpdated.getTime()));
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
}
