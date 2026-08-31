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

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;

/**
 * Phase 19 - Operational Governance, Fleet Reliability, Zero-Downtime Deployment,
 * API Contract Governance, Security Incident Response & Audit Integrity Verification Test.
 *
 * Real PostgreSQL Integration Test (150+ distinct assertions).
 */
public class Phase19OperationalGovernanceVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    private static final String DB_USER = "abizlocker";
    private static final String DB_PASS = "abiz_dev_secure_pass_9281";

    private static SqlSessionFactory sqlSessionFactory;
    private static DeviceCommandMapper commandMapper;
    private static FinancedDeviceMapper deviceMapper;
    private static DeploymentEventMapper deploymentMapper;
    private static BackupRecordMapper backupMapper;
    private static IncidentEventMapper incidentMapper;
    private static ApiContractMapper apiContractMapper;
    private static AgentUpgradeMapper agentUpgradeMapper;
    private static DeviceQuarantineMapper quarantineMapper;
    private static FeatureFlagMapper featureFlagMapper;
    private static BreakGlassMapper breakGlassMapper;
    private static AuditIntegrityMapper auditIntegrityMapper;
    private static SloMeasurementMapper sloMeasurementMapper;
    private static AlertEventMapper alertEventMapper;
    private static RateLimitCounterMapper rateLimitCounterMapper;
    private static SecurityTimelineMapper securityTimelineMapper;
    private static ShopMapper shopMapper;
    private static CustomerMapper customerMapper;
    private static AuditMapper auditMapper;
    private static AbizAuditService auditService;
    private static PooledDataSource dataSource;

    private static UUID testShopId;
    private static UUID testCustomerId;
    private static UUID testDeviceId;
    private static UUID testActorId;

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  ABIZ LOCKER - PHASE 19 OPERATIONAL GOVERNANCE VERIFICATION SUITE");
        System.out.println("==========================================================================");

        try {
            initEnvironment();

            // Run test groups
            test19_1_ApiContractsAndVersionGovernance();
            test19_2_ProtocolCompatibilityAndNegotiation();
            test19_3_AndroidFleetVersionManagement();
            test19_4_DeviceQuarantineAndFailClosed();
            test19_5_FeatureFlagsAndKillSwitches();
            test19_6_ZeroDowntimeDeploymentAndCanary();
            test19_7_PostDeploymentVerificationGate();
            test19_8_AdvancedQueueDurabilityAndPriority();
            test19_9_CircuitBreakers();
            test19_10_IncidentResponseAndEscalation();
            test19_11_SecurityEventForensicsAndTimeline();
            test19_12_AdministrativeBreakGlassAccess();
            test19_13_CryptographicAuditIntegrityChaining();
            test19_14_OperationalSloMonitoring();
            test19_15_AlertEscalationAndRouting();
            test19_16_DataRetentionAndSafetyPolicies();
            test19_17_DatabaseConnectionAndDeadlockRetry();
            test19_18_CacheConsistencyAndTenantIsolation();
            test19_19_DistributedRateLimitPersistence();
            test19_20_HealthDegradationAndConfigValidation();
            test19_21_MigrationSafetyGate();
            test19_22_ApiAbuseAndFuzzingDefense();
            test19_23_MasterCommandPolicyInvariantChain();

        } catch (Throwable t) {
            System.err.println("[FATAL] Suite setup failed: " + t.getMessage());
            t.printStackTrace();
            testsFailed++;
        }

        System.out.println("\n==========================================================================");
        System.out.println("  PHASE 19 VERIFICATION SUMMARY: " + testsPassed + " PASS, " + testsFailed + " FAIL");
        System.out.println("==========================================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void pass(String name) {
        testsPassed++;
        System.out.println("  [PASS] " + name);
    }

    private static void fail(String name, String reason) {
        testsFailed++;
        System.out.println("  [FAIL] " + name + ": " + reason);
    }

    private static void assertTrue(String name, boolean condition, String reason) {
        if (condition) pass(name);
        else fail(name, reason);
    }

    private static void assertEquals(String name, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) pass(name);
        else fail(name, "Expected '" + expected + "' but got '" + actual + "'");
    }

    private static void initEnvironment() throws Exception {
        dataSource = new PooledDataSource();
        dataSource.setDriver("org.postgresql.Driver");
        dataSource.setUrl(DB_URL);
        dataSource.setUsername(DB_USER);
        dataSource.setPassword(DB_PASS);
        dataSource.setPoolMaximumActiveConnections(10);

        Environment environment = new Environment("phase19-test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);

        configuration.addMapper(DeviceCommandMapper.class);
        configuration.addMapper(FinancedDeviceMapper.class);
        configuration.addMapper(DeploymentEventMapper.class);
        configuration.addMapper(BackupRecordMapper.class);
        configuration.addMapper(IncidentEventMapper.class);
        configuration.addMapper(ApiContractMapper.class);
        configuration.addMapper(AgentUpgradeMapper.class);
        configuration.addMapper(DeviceQuarantineMapper.class);
        configuration.addMapper(FeatureFlagMapper.class);
        configuration.addMapper(BreakGlassMapper.class);
        configuration.addMapper(AuditIntegrityMapper.class);
        configuration.addMapper(SloMeasurementMapper.class);
        configuration.addMapper(AlertEventMapper.class);
        configuration.addMapper(RateLimitCounterMapper.class);
        configuration.addMapper(SecurityTimelineMapper.class);
        configuration.addMapper(ShopMapper.class);
        configuration.addMapper(CustomerMapper.class);
        configuration.addMapper(AuditMapper.class);

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        SqlSession session = sqlSessionFactory.openSession(true);

        commandMapper = session.getMapper(DeviceCommandMapper.class);
        deviceMapper = session.getMapper(FinancedDeviceMapper.class);
        deploymentMapper = session.getMapper(DeploymentEventMapper.class);
        backupMapper = session.getMapper(BackupRecordMapper.class);
        incidentMapper = session.getMapper(IncidentEventMapper.class);
        apiContractMapper = session.getMapper(ApiContractMapper.class);
        agentUpgradeMapper = session.getMapper(AgentUpgradeMapper.class);
        quarantineMapper = session.getMapper(DeviceQuarantineMapper.class);
        featureFlagMapper = session.getMapper(FeatureFlagMapper.class);
        breakGlassMapper = session.getMapper(BreakGlassMapper.class);
        auditIntegrityMapper = session.getMapper(AuditIntegrityMapper.class);
        sloMeasurementMapper = session.getMapper(SloMeasurementMapper.class);
        alertEventMapper = session.getMapper(AlertEventMapper.class);
        rateLimitCounterMapper = session.getMapper(RateLimitCounterMapper.class);
        securityTimelineMapper = session.getMapper(SecurityTimelineMapper.class);
        shopMapper = session.getMapper(ShopMapper.class);
        customerMapper = session.getMapper(CustomerMapper.class);
        auditMapper = session.getMapper(AuditMapper.class);

        auditService = new AbizAuditService();
        Field f = AbizAuditService.class.getDeclaredField("auditMapper");
        f.setAccessible(true);
        f.set(auditService, auditMapper);

        testShopId = UUID.randomUUID();
        testCustomerId = UUID.randomUUID();
        testDeviceId = UUID.randomUUID();
        testActorId = UUID.randomUUID();

        // Create test shop
        AbizShop shop = new AbizShop();
        shop.setId(testShopId);
        shop.setName("Phase 19 Governance Test Shop");
        shop.setOwnerName("Shop Owner P19");
        shop.setEmail("p19_test_" + System.currentTimeMillis() + "@abiz.test");
        shop.setPhone("+923001919191");
        shop.setAddress("P19 Test Blvd");
        shop.setCity("Lahore");
        shop.setStatus("ACTIVE");
        shopMapper.insertShop(shop);

        // Create test customer
        AbizCustomer customer = new AbizCustomer();
        customer.setId(testCustomerId);
        customer.setShopId(testShopId);
        customer.setName("Customer P19");
        customer.setPhone("+923001919192");
        customer.setCnic("35201-1919191-1");
        customer.setStatus("ACTIVE");
        customerMapper.insertCustomer(customer);

        // Create test device
        AbizFinancedDevice device = new AbizFinancedDevice();
        device.setId(testDeviceId);
        device.setShopId(testShopId);
        device.setCustomerId(testCustomerId);
        device.setImei1("35" + (1000000000000L + System.currentTimeMillis() % 1000000000000L));
        device.setSerialNumber("SN-P19-" + System.currentTimeMillis());
        device.setManufacturer("Google");
        device.setModel("Pixel 8 P19");
        device.setAndroidVersion("14");
        device.setAgentVersion("0.1.19");
        device.setAgentVersionCode(19);
        device.setEnrollmentStatus("ENROLLED");
        device.setConnectionStatus("ONLINE");
        device.setLockStatus("UNLOCKED");
        device.setLicenseStatus("ACTIVE");
        device.setPolicyState("ACTIVE");
        device.setAgentProtocolVersion("v2");
        device.setIntegrityStatus("VERIFIED");
        device.setQuarantineStatus("ACTIVE");
        device.setDeviceOwnerActive(true);
        deviceMapper.insertDevice(device);
    }

    // 19.1 API Contracts & Version Governance (8 tests)
    private static void test19_1_ApiContractsAndVersionGovernance() {
        System.out.println("\n--- 19.1 API Contracts & Version Governance ---");
        ApiCompatibilityService compatService = new ApiCompatibilityService(apiContractMapper);

        assertTrue("19.1.1 Current API version is v2", ApiCompatibilityService.CURRENT_BACKEND_API_VERSION.equals("v2"), "");
        assertTrue("19.1.2 Agent protocol v2 is supported", compatService.isProtocolSupported("v2"), "");
        assertTrue("19.1.3 Agent protocol v1 is supported", compatService.isProtocolSupported("v1"), "");
        assertTrue("19.1.4 Agent protocol v0.1 is rejected", !compatService.isProtocolSupported("v0.1"), "");
        assertTrue("19.1.5 Agent protocol v99 is rejected", !compatService.isProtocolSupported("v99"), "");
        assertTrue("19.1.6 Protocol v1 is marked deprecated", compatService.isProtocolDeprecated("v1"), "");
        assertTrue("19.1.7 Protocol v2 is not deprecated", !compatService.isProtocolDeprecated("v2"), "");

        compatService.registerVersion("v2.0-GA", "v2", "v1", "v2");
        AbizApiContractVersion v = apiContractMapper.findByVersionName("v2.0-GA");
        assertTrue("19.1.8 API contract version persisted in DB", v != null && "v2".equals(v.getProtocolVersion()), "");
    }

    // 19.2 Protocol Compatibility & Negotiation (6 tests)
    private static void test19_2_ProtocolCompatibilityAndNegotiation() {
        System.out.println("\n--- 19.2 Protocol Compatibility & Negotiation ---");
        ApiCompatibilityService compatService = new ApiCompatibilityService(apiContractMapper);
        ProtocolNegotiationService negotiationService = new ProtocolNegotiationService(compatService);

        assertEquals("19.2.1 Negotiate v2 returns v2", "v2", negotiationService.negotiate("v2"));
        assertEquals("19.2.2 Negotiate v1 returns v1", "v1", negotiationService.negotiate("v1"));

        boolean thrown = false;
        try {
            negotiationService.negotiate("v99");
        } catch (SecurityException e) {
            thrown = true;
        }
        assertTrue("19.2.3 Unsupported protocol throws SecurityException", thrown, "");

        assertTrue("19.2.4 Command v2 with Device v2 is compatible", negotiationService.validateCommandProtocol("v2", "v2"), "");
        assertTrue("19.2.5 Command v1 with Device v2 is compatible", negotiationService.validateCommandProtocol("v1", "v2"), "");
        assertTrue("19.2.6 Command v99 with Device v2 is incompatible", !negotiationService.validateCommandProtocol("v99", "v2"), "");
    }

    // 19.3 Android Fleet Version Management (7 tests)
    private static void test19_3_AndroidFleetVersionManagement() {
        System.out.println("\n--- 19.3 Android Fleet Version Management ---");
        AgentCompatibilityService compat = new AgentCompatibilityService();
        AgentUpgradePolicyService policy = new AgentUpgradePolicyService(compat);
        AgentFleetService fleet = new AgentFleetService(deviceMapper, agentUpgradeMapper, policy);

        AbizFinancedDevice dev = deviceMapper.findDeviceById(testDeviceId);
        assertEquals("19.3.1 Device version code evaluates to CURRENT", AbizFinancedDevice.UPGRADE_STATUS_CURRENT, policy.evaluateUpgradeStatus(dev, 19));
        
        dev.setAgentVersionCode(1);
        assertEquals("19.3.2 Lower version code evaluates to UPDATE_AVAILABLE", AbizFinancedDevice.UPGRADE_STATUS_UPDATE_AVAILABLE, policy.evaluateUpgradeStatus(dev, 19));

        dev.setAgentVersionCode(0);
        assertEquals("19.3.3 Obsolete version code evaluates to UPDATE_REQUIRED", AbizFinancedDevice.UPGRADE_STATUS_UPDATE_REQUIRED, policy.evaluateUpgradeStatus(dev, 19));

        dev.setAgentVersionCode(19);
        assertTrue("19.3.4 Active device is upgrade eligible", policy.canUpgrade(dev), "");

        boolean initiated = fleet.initiateUpgrade(testDeviceId, "0.1.18", "0.1.19", 19);
        assertTrue("19.3.5 Upgrade initiated successfully", initiated, "");

        fleet.completeUpgrade(testDeviceId, null, "0.1.19", 19);
        AbizFinancedDevice after = deviceMapper.findDeviceById(testDeviceId);
        assertEquals("19.3.6 Upgrade completed updates status to CURRENT", AbizFinancedDevice.UPGRADE_STATUS_CURRENT, after.getUpgradeStatus());
        assertEquals("19.3.7 Upgrade completed updates version name", "0.1.19", after.getAgentVersion());
    }

    // 19.4 Device Quarantine & Fail-Closed Guard (7 tests)
    private static void test19_4_DeviceQuarantineAndFailClosed() {
        System.out.println("\n--- 19.4 Device Quarantine & Fail-Closed ---");
        DeviceQuarantineService quarantineService = new DeviceQuarantineService(deviceMapper, quarantineMapper);

        assertTrue("19.4.1 Device initially not quarantined", !quarantineService.isQuarantined(testDeviceId), "");

        boolean q = quarantineService.quarantineDevice(testDeviceId, testShopId, testActorId, "SUSPICIOUS_REPLAY_ATTACK", "50 duplicate nonces detected");
        assertTrue("19.4.2 Quarantine applied successfully", q, "");
        assertTrue("19.4.3 isQuarantined returns true after quarantine", quarantineService.isQuarantined(testDeviceId), "");
        assertEquals("19.4.4 Quarantine reason preserved", "SUSPICIOUS_REPLAY_ATTACK", quarantineService.getQuarantineReason(testDeviceId));

        List<AbizDeviceQuarantineEvent> events = quarantineMapper.findRecentByDevice(testDeviceId, 5);
        assertTrue("19.4.5 Quarantine event recorded in audit table", !events.isEmpty(), "");

        boolean rel = quarantineService.releaseFromQuarantine(testDeviceId, testShopId, testActorId, "ADMIN_VERIFIED_CLEAN");
        assertTrue("19.4.6 Release from quarantine successful", rel, "");
        assertTrue("19.4.7 isQuarantined returns false after release", !quarantineService.isQuarantined(testDeviceId), "");
    }

    // 19.5 Feature Flags & Emergency Kill Switches (8 tests)
    private static void test19_5_FeatureFlagsAndKillSwitches() {
        System.out.println("\n--- 19.5 Feature Flags & Emergency Kill Switches ---");
        FeatureFlagService flagService = new FeatureFlagService(featureFlagMapper);
        OperationalKillSwitchService killSwitch = new OperationalKillSwitchService(flagService);

        assertTrue("19.5.1 Default REMOTE_LOCK feature is enabled", flagService.isEnabled(AbizFeatureFlag.FLAG_REMOTE_LOCK, testShopId), "");

        flagService.setGlobalFlag(AbizFeatureFlag.FLAG_ADMIN_REPLAY, true, "Admin replay enabled globally", testActorId);
        assertTrue("19.5.2 Global flag persisted and enabled", flagService.isEnabled(AbizFeatureFlag.FLAG_ADMIN_REPLAY, null), "");

        flagService.setShopFlag(AbizFeatureFlag.FLAG_WIPE, testShopId, false, "Wipe disabled for test shop", testActorId);
        assertTrue("19.5.3 Shop-specific flag overrides global setting", !flagService.isEnabled(AbizFeatureFlag.FLAG_WIPE, testShopId), "");

        killSwitch.triggerEmergencyKillSwitch(AbizFeatureFlag.FLAG_REMOTE_COMMANDS, testActorId, "ACTIVE_SECURITY_INVESTIGATION");
        assertTrue("19.5.4 Emergency kill switch disables remote commands globally", !killSwitch.isOperationAllowed(AbizFeatureFlag.FLAG_REMOTE_COMMANDS, testShopId), "");

        killSwitch.restoreKillSwitch(AbizFeatureFlag.FLAG_REMOTE_COMMANDS, testActorId, "INVESTIGATION_RESOLVED");
        assertTrue("19.5.5 Restored kill switch re-enables operation", killSwitch.isOperationAllowed(AbizFeatureFlag.FLAG_REMOTE_COMMANDS, testShopId), "");

        assertTrue("19.5.6 FeatureFlag table query succeeds", featureFlagMapper.countAll() >= 0, "");
        AbizFeatureFlag gFlag = featureFlagMapper.findGlobalFlag(AbizFeatureFlag.FLAG_ADMIN_REPLAY);
        assertTrue("19.5.7 findGlobalFlag returns valid entity", gFlag != null && gFlag.isEnabled(), "");
        AbizFeatureFlag sFlag = featureFlagMapper.findShopFlag(AbizFeatureFlag.FLAG_WIPE, testShopId);
        assertTrue("19.5.8 findShopFlag returns valid entity", sFlag != null && !sFlag.isEnabled(), "");
    }

    // 19.6 Zero-Downtime Deployment & Canary Progression (8 tests)
    private static void test19_6_ZeroDowntimeDeploymentAndCanary() {
        System.out.println("\n--- 19.6 Zero-Downtime Deployment & Canary ---");
        DeploymentSafetyService safety = new DeploymentSafetyService(deploymentMapper, backupMapper, dataSource, auditService);

        AbizDeploymentEvent dep = safety.startDeployment("DEP-P19-" + System.currentTimeMillis(), "0.1.19", "production", testActorId);
        assertEquals("19.6.1 Deployment starts in PRECHECK stage", AbizDeploymentEvent.STATUS_PRECHECK, dep.getStatus());

        assertTrue("19.6.2 Transition PRECHECK -> APPROVED", safety.transitionStatus(dep, AbizDeploymentEvent.STATUS_APPROVED, "Preflight passed"), "");
        assertTrue("19.6.3 Transition APPROVED -> CANARY", safety.transitionStatus(dep, "CANARY", "Starting 1% canary"), "");
        assertTrue("19.6.4 Transition CANARY -> CANARY_VERIFY", safety.transitionStatus(dep, "CANARY_VERIFY", "Verifying canary health"), "");
        assertTrue("19.6.5 Transition CANARY_VERIFY -> ROLLING_OUT", safety.transitionStatus(dep, "ROLLING_OUT", "Rolling out to 100%"), "");
        assertTrue("19.6.6 Transition ROLLING_OUT -> ROLLING_VERIFY", safety.transitionStatus(dep, "ROLLING_VERIFY", "Verifying full rollout"), "");
        assertTrue("19.6.7 Transition ROLLING_VERIFY -> COMPLETED", safety.transitionStatus(dep, AbizDeploymentEvent.STATUS_COMPLETED, "Deployment finished"), "");

        assertTrue("19.6.8 Canary progression allows healthy error rate (0.01 < 0.05)", safety.evaluateCanaryProgression(5, 0.05, 0.01), "");
    }

    // 19.7 Post-Deployment Verification Gate (6 tests)
    private static void test19_7_PostDeploymentVerificationGate() {
        System.out.println("\n--- 19.7 Post-Deployment Verification Gate ---");
        HealthService health = new HealthService(commandMapper, deviceMapper);
        PostDeploymentVerificationService postVerif = new PostDeploymentVerificationService(commandMapper, deviceMapper, health);

        Map<String, Object> results = postVerif.verifyPostDeployment();
        assertTrue("19.7.1 Post-deployment results contain databaseLive", results.containsKey("databaseLive"), "");
        assertTrue("19.7.2 Database is live post-deployment", Boolean.TRUE.equals(results.get("databaseLive")), "");
        assertTrue("19.7.3 Database is ready post-deployment", Boolean.TRUE.equals(results.get("databaseReady")), "");
        assertTrue("19.7.4 Queue depth is non-negative", ((Number) results.get("queueDepth")).longValue() >= 0, "");
        assertTrue("19.7.5 Dead-letter count is non-negative", ((Number) results.get("deadLetteredCount")).longValue() >= 0, "");
        assertTrue("19.7.6 Overall verification status is true", Boolean.TRUE.equals(results.get("verified")), "");
    }

    // 19.8 Advanced Command Queue Durability & Priority (8 tests)
    private static void test19_8_AdvancedQueueDurabilityAndPriority() {
        System.out.println("\n--- 19.8 Advanced Queue Durability & Priority ---");

        AbizDeviceCommand cmdNormal = createTestCommand(testDeviceId, testShopId, "LOCK", "NORMAL");
        AbizDeviceCommand cmdCritical = createTestCommand(testDeviceId, testShopId, "LOCK", "CRITICAL");
        AbizDeviceCommand cmdHigh = createTestCommand(testDeviceId, testShopId, "LOCK", "HIGH");

        commandMapper.insertCommand(cmdNormal);
        commandMapper.insertCommand(cmdCritical);
        commandMapper.insertCommand(cmdHigh);

        List<AbizDeviceCommand> priorityList = commandMapper.findPendingByPriority(10);
        assertTrue("19.8.1 Pending commands found by priority", !priorityList.isEmpty(), "");
        assertTrue("19.8.2 CRITICAL priority command claimed first", "CRITICAL".equals(priorityList.get(0).getPriority()), "");

        int claimed = commandMapper.claimCommandWithWorker(cmdCritical.getId(), "worker-node-p19");
        assertTrue("19.8.3 Worker claim successful with worker ID", claimed == 1, "");

        AbizDeviceCommand claimedCmd = commandMapper.findCommandById(cmdCritical.getId());
        assertEquals("19.8.4 Claimed command has status DISPATCHING", "DISPATCHING", claimedCmd.getStatus());
        assertEquals("19.8.5 Claimed command has lastWorkerId set", "worker-node-p19", claimedCmd.getLastWorkerId());
        assertTrue("19.8.6 Claimed command has processingStartedAt set", claimedCmd.getProcessingStartedAt() != null, "");

        assertTrue("19.8.7 countCommandsByPriority for CRITICAL is valid", commandMapper.countCommandsByPriority("CRITICAL") >= 1, "");
        assertTrue("19.8.8 countCommandsByPriority for NORMAL is valid", commandMapper.countCommandsByPriority("NORMAL") >= 1, "");
    }

    // 19.9 Circuit Breakers (9 tests)
    private static void test19_9_CircuitBreakers() {
        System.out.println("\n--- 19.9 Circuit Breakers ---");
        CommandTransportCircuitBreaker transportCb = new CommandTransportCircuitBreaker(3, 100L);
        DatabaseCircuitBreaker dbCb = new DatabaseCircuitBreaker(3, 100L);
        AndroidAgentCircuitBreaker agentCb = new AndroidAgentCircuitBreaker(3, 100L);

        assertTrue("19.9.1 Transport circuit breaker starts CLOSED", transportCb.getState() == CommandTransportCircuitBreaker.State.CLOSED, "");
        assertTrue("19.9.2 Initial requests allowed", transportCb.allowRequest(), "");

        transportCb.recordFailure();
        transportCb.recordFailure();
        transportCb.recordFailure();
        assertEquals("19.9.3 Transport CB transitions to OPEN after 3 failures", CommandTransportCircuitBreaker.State.OPEN, transportCb.getState());
        assertTrue("19.9.4 Requests blocked when OPEN", !transportCb.allowRequest(), "");

        try { Thread.sleep(120L); } catch (InterruptedException ignored) {}
        assertTrue("19.9.5 Transport CB allows probe after timeout (HALF_OPEN)", transportCb.allowRequest(), "");
        assertEquals("19.9.6 State is HALF_OPEN during probe", CommandTransportCircuitBreaker.State.HALF_OPEN, transportCb.getState());

        transportCb.recordSuccess();
        assertEquals("19.9.7 Successful probe resets CB to CLOSED", CommandTransportCircuitBreaker.State.CLOSED, transportCb.getState());

        assertTrue("19.9.8 DB circuit breaker starts CLOSED", dbCb.getState() == DatabaseCircuitBreaker.State.CLOSED, "");
        assertTrue("19.9.9 Android agent circuit breaker starts CLOSED", agentCb.getState() == AndroidAgentCircuitBreaker.State.CLOSED, "");
    }

    // 19.10 Incident Response & Automatic Escalation (7 tests)
    private static void test19_10_IncidentResponseAndEscalation() {
        System.out.println("\n--- 19.10 Incident Response & Escalation ---");
        IncidentCorrelationService corr = new IncidentCorrelationService(incidentMapper);
        IncidentEscalationService esc = new IncidentEscalationService(incidentMapper);
        IncidentResolutionService res = new IncidentResolutionService(incidentMapper);

        assertEquals("19.10.1 Single replay attempt evaluates to LOW", AbizIncidentEvent.SEVERITY_LOW, esc.computeSeverity(1, "REPLAY_ATTACK"));
        assertEquals("19.10.2 15 replay attempts evaluate to MEDIUM", AbizIncidentEvent.SEVERITY_MEDIUM, esc.computeSeverity(15, "REPLAY_ATTACK"));
        assertEquals("19.10.3 60 replay attempts evaluate to HIGH", AbizIncidentEvent.SEVERITY_HIGH, esc.computeSeverity(60, "REPLAY_ATTACK"));
        assertEquals("19.10.4 Integrity failure evaluates to CRITICAL", AbizIncidentEvent.SEVERITY_CRITICAL, esc.computeSeverity(1, "INTEGRITY_FAILURE"));

        AbizIncidentEvent inc = corr.correlateOrRaise("REPLAY_STORM", "HIGH", testShopId, testDeviceId, "CORR-P19-10", "50 replay attempts");
        assertTrue("19.10.5 Incident created and persisted", inc != null && inc.getId() != null, "");

        boolean contained = res.containIncident(inc.getId());
        assertTrue("19.10.6 Incident contained successfully", contained, "");

        boolean resolved = res.resolveIncident(inc.getId(), "Attacker IP blocked and keys rotated");
        assertTrue("19.10.7 Incident resolved successfully", resolved, "");
    }

    // 19.11 Security Event Forensics & Timeline (5 tests)
    private static void test19_11_SecurityEventForensicsAndTimeline() {
        System.out.println("\n--- 19.11 Security Event Forensics & Timeline ---");
        SecurityForensicsService forensics = new SecurityForensicsService(securityTimelineMapper);

        String corrId = "CORR-FORENSIC-P19-" + System.currentTimeMillis();
        forensics.recordTimelineStage(corrId, UUID.randomUUID(), testShopId, testDeviceId, testActorId, "COMMAND_DISPATCH", "AUTHORIZATION", "User authorized with token secret_token_12345");
        forensics.recordTimelineStage(corrId, UUID.randomUUID(), testShopId, testDeviceId, testActorId, "COMMAND_DISPATCH", "DISPATCH", "Dispatched to push gateway");
        forensics.recordTimelineStage(corrId, UUID.randomUUID(), testShopId, testDeviceId, testActorId, "COMMAND_DISPATCH", "EXECUTION_ACK", "Device confirmed lockNow()");

        List<AbizSecurityTimeline> timeline = forensics.findSecurityTimeline(corrId);
        assertEquals("19.11.1 Reconstructed timeline has 3 stages", 3, timeline.size());
        assertEquals("19.11.2 Stage 1 is AUTHORIZATION", "AUTHORIZATION", timeline.get(0).getStage());
        assertEquals("19.11.3 Stage 2 is DISPATCH", "DISPATCH", timeline.get(1).getStage());
        assertEquals("19.11.4 Stage 3 is EXECUTION_ACK", "EXECUTION_ACK", timeline.get(2).getStage());
        assertTrue("19.11.5 Secrets in payload are redacted in timeline", !timeline.get(0).getPayloadRedacted().contains("secret_token_12345"), "");
    }

    // 19.12 Administrative Break-Glass Access (7 tests)
    private static void test19_12_AdministrativeBreakGlassAccess() {
        System.out.println("\n--- 19.12 Administrative Break-Glass Access ---");
        BreakGlassService breakGlass = new BreakGlassService(breakGlassMapper);

        AbizBreakGlassSession session = breakGlass.grantSession(testActorId, testShopId, BreakGlassService.SCOPE_DEVICE_COMMAND_REPLAY, "Emergency recovery of lost ACK", 15);
        assertTrue("19.12.1 Break-glass session granted", session != null && session.getId() != null, "");
        assertTrue("19.12.2 Break-glass session valid", session.isSessionValid(), "");
        assertTrue("19.12.3 Access validated for granted scope", breakGlass.validateAccess(testActorId, BreakGlassService.SCOPE_DEVICE_COMMAND_REPLAY), "");
        assertTrue("19.12.4 Access denied for ungranted scope", !breakGlass.validateAccess(testActorId, "UNAUTHORIZED_SCOPE"), "");

        boolean wipeBlocked = false;
        try {
            breakGlass.grantSession(testActorId, testShopId, "WIPE_ALL_DEVICES", "Prohibited attempt", 15);
        } catch (SecurityException e) {
            wipeBlocked = true;
        }
        assertTrue("19.12.5 Break-glass strictly denies WIPE permissions", wipeBlocked, "");

        breakGlass.revokeSession(session.getId());
        AbizBreakGlassSession revoked = breakGlassMapper.findById(session.getId());
        assertEquals("19.12.6 Session revoked updates status", AbizBreakGlassSession.STATUS_REVOKED, revoked.getStatus());
        assertTrue("19.12.7 Access denied after revocation", !breakGlass.validateAccess(testActorId, BreakGlassService.SCOPE_DEVICE_COMMAND_REPLAY), "");
    }

    // 19.13 Cryptographic Audit Integrity Chaining (7 tests)
    private static void test19_13_CryptographicAuditIntegrityChaining() {
        System.out.println("\n--- 19.13 Cryptographic Audit Integrity Chaining ---");
        AuditIntegrityService integrityService = new AuditIntegrityService(auditIntegrityMapper);

        String hash1 = AuditIntegrityService.computeHash(AuditIntegrityService.GENESIS_HASH, "AUDIT_EVENT_1: CREATE_COMMAND");
        assertTrue("19.13.1 Hash1 is 64-char hex SHA-256", hash1 != null && hash1.length() == 64, "");

        String hash2 = AuditIntegrityService.computeHash(hash1, "AUDIT_EVENT_2: EXECUTE_LOCK");
        assertTrue("19.13.2 Hash2 is 64-char hex SHA-256", hash2 != null && hash2.length() == 64, "");
        assertTrue("19.13.3 Chained hash differs from preceding hash", !hash1.equals(hash2), "");

        AbizAuditIntegrity rec1 = integrityService.recordEvent(1001L, "TEST_PAYLOAD_1");
        assertTrue("19.13.4 Integrity record 1 created", rec1 != null && rec1.getSequenceNum() >= 1, "");

        AbizAuditIntegrity rec2 = integrityService.recordEvent(1002L, "TEST_PAYLOAD_2");
        assertTrue("19.13.5 Integrity record 2 created with sequential number", rec2.getSequenceNum() == rec1.getSequenceNum() + 1, "");
        assertEquals("19.13.6 Record 2 previousHash links to record 1 eventHash", rec1.getEventHash(), rec2.getPreviousHash());

        assertTrue("19.13.7 Chain verification passes on unmodified chain", integrityService.verifyChain(10), "");
    }

    // 19.14 Operational SLO Monitoring (6 tests)
    private static void test19_14_OperationalSloMonitoring() {
        System.out.println("\n--- 19.14 Operational SLO Monitoring ---");
        OperationalSloService sloService = new OperationalSloService(sloMeasurementMapper);

        AbizSloMeasurement mHealthy = sloService.recordMeasurement(OperationalSloService.SLO_COMMAND_EXECUTION_SUCCESS, 0.99, 0.995);
        assertEquals("19.14.1 Actual >= Target evaluates to HEALTHY", AbizSloMeasurement.STATUS_HEALTHY, mHealthy.getStatus());

        AbizSloMeasurement mWarning = sloService.recordMeasurement(OperationalSloService.SLO_COMMAND_EXECUTION_SUCCESS, 0.99, 0.95);
        assertEquals("19.14.2 Actual slightly below Target evaluates to WARNING", AbizSloMeasurement.STATUS_WARNING, mWarning.getStatus());

        AbizSloMeasurement mCritical = sloService.recordMeasurement(OperationalSloService.SLO_COMMAND_EXECUTION_SUCCESS, 0.99, 0.80);
        assertEquals("19.14.3 Actual < 0.9*Target evaluates to CRITICAL", AbizSloMeasurement.STATUS_CRITICAL, mCritical.getStatus());

        assertTrue("19.14.4 isSloHealthy returns false for critical metric", !sloService.isSloHealthy(OperationalSloService.SLO_COMMAND_EXECUTION_SUCCESS), "");
        
        AbizSloMeasurement mDevice = sloService.recordMeasurement(OperationalSloService.SLO_DEVICE_HEARTBEAT_AVAILABILITY, 0.98, 0.99);
        assertTrue("19.14.5 isSloHealthy returns true for healthy metric", sloService.isSloHealthy(OperationalSloService.SLO_DEVICE_HEARTBEAT_AVAILABILITY), "");

        assertTrue("19.14.6 SLO measurements persisted in DB", sloMeasurementMapper.countAll() >= 4, "");
    }

    // 19.15 Alert Escalation & Severity Routing (6 tests)
    private static void test19_15_AlertEscalationAndRouting() {
        System.out.println("\n--- 19.15 Alert Escalation & Severity Routing ---");
        AlertService alertService = new AlertService(commandMapper, deviceMapper, auditService);

        boolean infoRouted = alertService.routeAlertSeverity("INFO", "INFO_METRIC_NORMAL", "Routine telemetry tick");
        assertTrue("19.15.1 INFO severity does not trigger alert dispatch notification", !infoRouted, "");

        boolean warnRouted = alertService.routeAlertSeverity("WARNING", "WARN_QUEUE_GROWING", "Queue depth reached 50");
        assertTrue("19.15.2 WARNING severity triggers alert dispatch", warnRouted, "");

        boolean criticalRouted = alertService.routeAlertSeverity("CRITICAL", "CRIT_SECURITY_BREACH", "Replay storm detected");
        assertTrue("19.15.3 CRITICAL severity triggers alert dispatch", criticalRouted, "");

        boolean deduped = alertService.routeAlertSeverity("CRITICAL", "CRIT_SECURITY_BREACH", "Replay storm duplicate tick");
        assertTrue("19.15.4 Duplicate alert within 5 minutes is suppressed/deduplicated", !deduped, "");

        AlertService.AlertEntry entry = alertService.getAlert("CRIT_SECURITY_BREACH");
        assertEquals("19.15.5 Suppressed alert increments occurrence counter to 2", 2, entry.occurrenceCount);

        alertService.clearAlerts();
        assertEquals("19.15.6 clearAlerts clears in-memory state", 0, alertService.getActiveAlertCount());
    }

    // 19.16 Data Retention & Safety Policies (6 tests)
    private static void test19_16_DataRetentionAndSafetyPolicies() {
        System.out.println("\n--- 19.16 Data Retention & Safety Policies ---");
        DataRetentionService retention = new DataRetentionService(dataSource, auditService);

        assertTrue("19.16.1 EXPIRED commands are safe to purge", retention.isSafeToPurge(DataRetentionService.POLICY_COMMAND_RETENTION, "EXPIRED"), "");
        assertTrue("19.16.2 CANCELLED commands are safe to purge", retention.isSafeToPurge(DataRetentionService.POLICY_COMMAND_RETENTION, "CANCELLED"), "");
        assertTrue("19.16.3 PENDING commands are protected from purge", !retention.isSafeToPurge(DataRetentionService.POLICY_COMMAND_RETENTION, "PENDING"), "");
        assertTrue("19.16.4 EXECUTED commands are protected from purge", !retention.isSafeToPurge(DataRetentionService.POLICY_COMMAND_RETENTION, "EXECUTED"), "");
        assertTrue("19.16.5 AUDIT records are strictly protected for hash chain integrity", !retention.isSafeToPurge(DataRetentionService.POLICY_AUDIT_RETENTION, "ARCHIVED"), "");
        assertTrue("19.16.6 RESOLVED incidents are safe to purge after retention window", retention.isSafeToPurge(DataRetentionService.POLICY_INCIDENT_RETENTION, "RESOLVED"), "");
    }

    // 19.17 Database Connection & Deadlock Retry (4 tests)
    private static void test19_17_DatabaseConnectionAndDeadlockRetry() {
        System.out.println("\n--- 19.17 Database Connection & Deadlock Retry ---");

        try (Connection conn = dataSource.getConnection()) {
            assertTrue("19.17.1 Real PostgreSQL connection is valid", conn.isValid(2), "");
            assertTrue("19.17.2 Transaction isolation satisfies READ_COMMITTED", DatabaseSafetyService.validateIsolation(conn, Connection.TRANSACTION_READ_COMMITTED), "");
        } catch (Exception e) {
            fail("19.17.1 Real PostgreSQL connection is valid", e.getMessage());
        }

        try {
            int result = DatabaseSafetyService.executeWithDeadlockRetry(() -> 42);
            assertEquals("19.17.3 executeWithDeadlockRetry returns computation result", 42, result);
        } catch (Exception e) {
            fail("19.17.3 executeWithDeadlockRetry returns computation result", e.getMessage());
        }

        assertEquals("19.17.4 Max deadlock retries constant is 3", 3, DatabaseSafetyService.MAX_DEADLOCK_RETRIES);
    }

    // 19.18 Cache Consistency & Tenant Isolation (5 tests)
    private static void test19_18_CacheConsistencyAndTenantIsolation() {
        System.out.println("\n--- 19.18 Cache Consistency & Tenant Isolation ---");
        CacheConsistencyService cache = new CacheConsistencyService();

        UUID shopA = UUID.randomUUID();
        UUID shopB = UUID.randomUUID();
        UUID dev1 = UUID.randomUUID();

        cache.put(shopA, dev1, "LOCK_STATUS", "LOCKED");
        cache.put(shopB, dev1, "LOCK_STATUS", "UNLOCKED");

        assertEquals("19.18.1 Tenant A cache isolated", "LOCKED", cache.get(shopA, dev1, "LOCK_STATUS"));
        assertEquals("19.18.2 Tenant B cache isolated", "UNLOCKED", cache.get(shopB, dev1, "LOCK_STATUS"));

        cache.invalidate(shopA, dev1, "LOCK_STATUS");
        assertTrue("19.18.3 Invalidate removes specific key", cache.get(shopA, dev1, "LOCK_STATUS") == null, "");
        assertTrue("19.18.4 Invalidate does not affect other tenants", cache.get(shopB, dev1, "LOCK_STATUS") != null, "");

        cache.clear();
        assertTrue("19.18.5 Clear empties entire cache", cache.get(shopB, dev1, "LOCK_STATUS") == null, "");
    }

    // 19.19 Distributed Rate Limit Persistence (5 tests)
    private static void test19_19_DistributedRateLimitPersistence() {
        System.out.println("\n--- 19.19 Distributed Rate Limit Persistence ---");
        PersistentRateLimitStore store = new PersistentRateLimitStore(rateLimitCounterMapper);

        String rateKey = "RATE_LIMIT_TEST_" + System.currentTimeMillis();
        assertTrue("19.19.1 First request within limit is allowed", store.isAllowed(rateKey, 3, 60000L), "");
        assertTrue("19.19.2 Second request within limit is allowed", store.isAllowed(rateKey, 3, 60000L), "");
        assertTrue("19.19.3 Third request within limit is allowed", store.isAllowed(rateKey, 3, 60000L), "");
        assertTrue("19.19.4 Fourth request exceeding limit is blocked", !store.isAllowed(rateKey, 3, 60000L), "");

        assertEquals("19.19.5 Hit count reflects exactly 3 allowed hits", 3, store.getHitCount(rateKey));
    }

    // 19.20 Health Degradation & Startup Config Validation (6 tests)
    private static void test19_20_HealthDegradationAndConfigValidation() {
        System.out.println("\n--- 19.20 Health Degradation & Config Validation ---");
        HealthService health = new HealthService(commandMapper, deviceMapper);

        assertEquals("19.20.1 Normal parameters return FULLY_OPERATIONAL", HealthService.STATE_FULLY_OPERATIONAL, health.getDegradationState(true, false, false));
        assertEquals("19.20.2 DB down returns DATABASE_UNAVAILABLE", HealthService.STATE_DATABASE_UNAVAILABLE, health.getDegradationState(false, false, false));
        assertEquals("19.20.3 Security lockdown returns SECURITY_LOCKDOWN", HealthService.STATE_SECURITY_LOCKDOWN, health.getDegradationState(true, true, false));
        assertEquals("19.20.4 Blocked queue returns COMMANDS_DISABLED", HealthService.STATE_COMMANDS_DISABLED, health.getDegradationState(true, false, true));

        Map<String, String> validConfig = new HashMap<>();
        validConfig.put("db.url", "jdbc:postgresql://localhost:3000/abizlocker");
        validConfig.put("db.user", "abizlocker");
        validConfig.put("jwt.secret", "a_very_secure_32_character_jwt_secret_key_12345");
        validConfig.put("api.protocol", "v2");
        assertTrue("19.20.5 Valid startup configuration passes validation", ProductionConfigurationValidator.validateStartupConfiguration(validConfig), "");

        Map<String, String> invalidConfig = new HashMap<>(validConfig);
        invalidConfig.put("db.user", "postgres"); // default prohibited
        assertTrue("19.20.6 Default 'postgres' user fails validation", !ProductionConfigurationValidator.validateStartupConfiguration(invalidConfig), "");
    }

    // 19.21 Migration Safety Gate (4 tests)
    private static void test19_21_MigrationSafetyGate() {
        System.out.println("\n--- 19.21 Migration Safety Gate ---");
        MigrationSafetyService migService = new MigrationSafetyService(commandMapper);

        assertTrue("19.21.1 Schema is operational against live PostgreSQL", migService.isSchemaOperational(), "");
        assertTrue("19.21.2 Migration gate passes when backup verified and schema compatible", migService.validateMigrationGate(true, true), "");
        assertTrue("19.21.3 Migration gate fails if backup unverified", !migService.validateMigrationGate(false, true), "");
        assertTrue("19.21.4 Migration gate fails if schema incompatible", !migService.validateMigrationGate(true, false), "");
    }

    // 19.22 API Abuse & Fuzzing Defense (9 tests)
    private static void test19_22_ApiAbuseAndFuzzingDefense() {
        System.out.println("\n--- 19.22 API Abuse & Fuzzing Defense ---");

        assertTrue("19.22.1 Valid UUID check passes", ApiContractValidator.isValidUuid(UUID.randomUUID().toString()), "");
        assertTrue("19.22.2 Malformed UUID string check fails", !ApiContractValidator.isValidUuid("not-a-valid-uuid-1234"), "");
        assertTrue("19.22.3 Null UUID string check fails", !ApiContractValidator.isValidUuid(null), "");

        assertTrue("19.22.4 Valid RequestId format check passes", ApiContractValidator.isValidRequestId("REQ-1234567890"), "");
        assertTrue("19.22.5 Invalid RequestId format check fails", !ApiContractValidator.isValidRequestId("INVALID-ID"), "");

        assertTrue("19.22.6 Valid 32-char hex nonce check passes", ApiContractValidator.isValidNonce("0123456789abcdef0123456789abcdef"), "");
        assertTrue("19.22.7 Short nonce check fails", !ApiContractValidator.isValidNonce("shortnonce"), "");

        boolean crossTenantBlocked = false;
        try {
            ApiAbuseDefenseService.validateTenantAndDevice(UUID.randomUUID(), UUID.randomUUID());
        } catch (SecurityException e) {
            crossTenantBlocked = true;
        }
        assertTrue("19.22.8 Cross-tenant access attempt throws SecurityException", crossTenantBlocked, "");

        boolean validTenantPassed = false;
        try {
            UUID sameTenant = UUID.randomUUID();
            ApiAbuseDefenseService.validateTenantAndDevice(sameTenant, sameTenant);
            validTenantPassed = true;
        } catch (SecurityException e) {
            validTenantPassed = false;
        }
        assertTrue("19.22.9 Same tenant access allowed", validTenantPassed, "");
    }

    // 19.23 Master Command Policy Invariant Chain (15 tests)
    private static void test19_23_MasterCommandPolicyInvariantChain() {
        System.out.println("\n--- 19.23 Master Command Policy Invariant Chain ---");
        Date futureDate = new Date(System.currentTimeMillis() + 60000L);
        Date pastDate = new Date(System.currentTimeMillis() - 60000L);
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();

        // 1. Full green invariant check
        CommandPolicyInvariant.InvariantCheckResult resAllPass = CommandPolicyInvariant.evaluateCommand(
                true, true, t1, t1, true, true, true, futureDate, false, true, true, true, true, true, true
        );
        assertTrue("19.23.1 Invariant passes when all 15 conditions are satisfied", resAllPass.passed, "");

        // 2. Unauthenticated
        assertEquals("19.23.2 Fail-closed on unauthenticated", "NOT_AUTHENTICATED",
                CommandPolicyInvariant.evaluateCommand(false, true, t1, t1, true, true, true, futureDate, false, true, true, true, true, true, true).failureReason);

        // 3. Unauthorized
        assertEquals("19.23.3 Fail-closed on unauthorized", "NOT_AUTHORIZED",
                CommandPolicyInvariant.evaluateCommand(true, false, t1, t1, true, true, true, futureDate, false, true, true, true, true, true, true).failureReason);

        // 4. Tenant mismatch
        assertEquals("19.23.4 Fail-closed on tenant mismatch", "TENANT_MISMATCH",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t2, true, true, true, futureDate, false, true, true, true, true, true, true).failureReason);

        // 5. License invalid
        assertEquals("19.23.5 Fail-closed on invalid license", "LICENSE_INVALID",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, false, true, true, futureDate, false, true, true, true, true, true, true).failureReason);

        // 6. Policy invalid
        assertEquals("19.23.6 Fail-closed on invalid policy", "POLICY_INVALID",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, false, true, futureDate, false, true, true, true, true, true, true).failureReason);

        // 7. Protocol incompatible
        assertEquals("19.23.7 Fail-closed on incompatible protocol", "PROTOCOL_INCOMPATIBLE",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, true, false, futureDate, false, true, true, true, true, true, true).failureReason);

        // 8. Command expired
        assertEquals("19.23.8 Fail-closed on expired command", "COMMAND_EXPIRED",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, true, true, pastDate, false, true, true, true, true, true, true).failureReason);

        // 9. Quarantined device
        assertEquals("19.23.9 Fail-closed on quarantined device", "DEVICE_QUARANTINED",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, true, true, futureDate, true, true, true, true, true, true, true).failureReason);

        // 10. Feature disabled
        assertEquals("19.23.10 Fail-closed on disabled feature flag", "FEATURE_DISABLED",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, true, true, futureDate, false, false, true, true, true, true, true).failureReason);

        // 11. Circuit breaker open
        assertEquals("19.23.11 Fail-closed on circuit breaker open", "CIRCUIT_BREAKER_OPEN",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, true, true, futureDate, false, true, false, true, true, true, true).failureReason);

        // 12. Idempotency violation
        assertEquals("19.23.12 Fail-closed on duplicate nonce / idempotency violation", "IDEMPOTENCY_VIOLATION",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, true, true, futureDate, false, true, true, false, true, true, true).failureReason);

        // 13. Audit failure
        assertEquals("19.23.13 Fail-closed on audit logging failure", "AUDIT_FAILED",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, true, true, futureDate, false, true, true, true, false, true, true).failureReason);

        // 14. Unobservable
        assertEquals("19.23.14 Fail-closed on unobservable telemetry", "UNOBSERVABLE",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, true, true, futureDate, false, true, true, true, true, false, true).failureReason);

        // 15. Unrecoverable
        assertEquals("19.23.15 Fail-closed on unrecoverable state", "UNRECOVERABLE",
                CommandPolicyInvariant.evaluateCommand(true, true, t1, t1, true, true, true, futureDate, false, true, true, true, true, true, false).failureReason);
    }

    private static AbizDeviceCommand createTestCommand(UUID deviceId, UUID shopId, String type, String priority) {
        AbizDeviceCommand cmd = new AbizDeviceCommand();
        cmd.setId(UUID.randomUUID());
        cmd.setShopId(shopId);
        cmd.setFinancedDeviceId(deviceId);
        cmd.setCommandType(type);
        cmd.setStatus("PENDING");
        cmd.setPayload("{\"action\":\"" + type + "\"}");
        cmd.setIdempotencyKey("IDEM-P19-" + UUID.randomUUID());
        cmd.setCorrelationId("CORR-P19-" + UUID.randomUUID());
        cmd.setNonce(UUID.randomUUID().toString().replace("-", ""));
        cmd.setRequestId("REQ-P19-" + System.currentTimeMillis());
        cmd.setProtocolVersion(2);
        cmd.setPriority(priority);
        cmd.setQueuePartition("DEFAULT");
        cmd.setAttemptCount(0);
        cmd.setMaxAttempts(3);
        cmd.setCreatedAt(new Date());
        cmd.setUpdatedAt(new Date());
        cmd.setExpiresAt(new Date(System.currentTimeMillis() + 300000L));
        return cmd;
    }
}
