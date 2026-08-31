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

import java.util.*;

public class Phase22DeploymentGovernanceVerificationTest {
    static int testsPassed = 0;
    static int testsFailed = 0;

    static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    static final String DB_USER = "abizlocker";
    static final String DB_PASS = "abiz_dev_secure_pass_9281";

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================================");
        System.out.println("SUITE 34: PHASE 22 DEPLOYMENT GOVERNANCE VERIFICATION TEST (150+)");
        System.out.println("==================================================================");

        PooledDataSource ds = new PooledDataSource();
        ds.setDriver("org.postgresql.Driver");
        ds.setUrl(DB_URL);
        ds.setUsername(DB_USER);
        ds.setPassword(DB_PASS);
        ds.setPoolMaximumActiveConnections(10);

        Environment env = new Environment("phase22", new JdbcTransactionFactory(), ds);
        Configuration config = new Configuration(env);
        config.setMapUnderscoreToCamelCase(true);
        config.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);

        config.addMapper(DeploymentReleaseMapper.class);
        config.addMapper(DeploymentCheckpointMapper.class);
        config.addMapper(RecoveryPlanMapper.class);
        config.addMapper(RecoveryExecutionMapper.class);
        config.addMapper(BackupVerificationMapper.class);
        config.addMapper(ConfigurationDriftMapper.class);
        config.addMapper(DeviceLifecycleMapper.class);
        config.addMapper(SecurityIncidentMapper.class);
        config.addMapper(AuditMapper.class);
        config.addMapper(ShopMapper.class);
        config.addMapper(FinancedDeviceMapper.class);
        config.addMapper(ConfigurationVersionMapper.class);
        config.addMapper(WorkerHeartbeatMapper.class);
        config.addMapper(SloMeasurementMapper.class);

        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

        try (SqlSession session = factory.openSession(true)) {
            DeploymentReleaseMapper releaseMapper = session.getMapper(DeploymentReleaseMapper.class);
            DeploymentCheckpointMapper checkpointMapper = session.getMapper(DeploymentCheckpointMapper.class);
            AuditMapper auditMapper = session.getMapper(AuditMapper.class);
            ConfigurationVersionMapper configMapper = session.getMapper(ConfigurationVersionMapper.class);
            WorkerHeartbeatMapper workerMapper = session.getMapper(WorkerHeartbeatMapper.class);
            SloMeasurementMapper sloMapper = session.getMapper(SloMeasurementMapper.class);
            SecurityIncidentMapper incidentMapper = session.getMapper(SecurityIncidentMapper.class);
            BackupVerificationMapper backupMapper = session.getMapper(BackupVerificationMapper.class);

            AbizAuditService auditService = new AbizAuditService();
            DeploymentGovernanceService govService = new DeploymentGovernanceService(releaseMapper, checkpointMapper, auditService);
            DeploymentPreflightService preflightService = new DeploymentPreflightService(releaseMapper, backupMapper, configMapper, workerMapper, sloMapper, incidentMapper);
            DeploymentVerificationService verService = new DeploymentVerificationService(govService, checkpointMapper);

            // 1. Release Registration (20 tests)
            for (int i = 1; i <= 20; i++) {
                String sha = DeploymentGovernanceService.computeSha256("artifact_test_" + i);
                AbizDeploymentRelease r = govService.registerRelease("v22.0." + i, "BACKEND_WAR", "launcher.war", sha, 42700000L, "operator_1", "CANARY");
                assertNotNull("34.1." + i + " Release registered with UUID", r.getId());
            }

            // 2. Four-Eyes Approval Enforcement (20 tests)
            for (int i = 1; i <= 20; i++) {
                String sha = DeploymentGovernanceService.computeSha256("approval_test_" + i);
                AbizDeploymentRelease r1 = govService.registerRelease("v22.1.a." + i, "BACKEND_WAR", "launcher.war", sha, 42700000L, "operator_1", "PRODUCTION");
                boolean selfApprove = govService.approveRelease(r1.getId(), "operator_1");
                assertFalse("34.2." + i + ".a Self-approval blocked", selfApprove);

                AbizDeploymentRelease r2 = govService.registerRelease("v22.1.b." + i, "BACKEND_WAR", "launcher.war", sha, 42700000L, "operator_1", "PRODUCTION");
                boolean peerApprove = govService.approveRelease(r2.getId(), "operator_2_lead");
                assertTrue("34.2." + i + ".b Four-eyes peer approval succeeded", peerApprove);
            }

            // 3. Preflight Checklist Evaluation (20 tests)
            for (int i = 1; i <= 20; i++) {
                String warSha = DeploymentGovernanceService.computeSha256("war_preflight_" + i);
                String apkSha = DeploymentGovernanceService.computeSha256("apk_preflight_" + i);
                Map<String, Boolean> checklist = preflightService.evaluatePreflightChecklist(warSha, apkSha);
                boolean pass = preflightService.isPreflightPassed(checklist);
                if (!pass) {
                    System.out.println("PREFLIGHT FAILED on iteration " + i + ": " + checklist);
                }
                assertTrue("34.3." + i + " Preflight checklist evaluated", pass);
            }

            // 4. Deployment Checkpoint Ingestion & Verification (40 tests)
            String relSha = DeploymentGovernanceService.computeSha256("checkpoint_rel");
            AbizDeploymentRelease rel = govService.registerRelease("v22.2.0", "BACKEND_WAR", "launcher.war", relSha, 42700000L, "operator_1", "PRODUCTION");
            govService.approveRelease(rel.getId(), "lead_approver");
            govService.startDeployment(rel.getId());

            String[] chkTypes = {"DATABASE_SCHEMA", "WAR_HASH", "APK_HASH", "VERSION", "CONFIGURATION",
                                 "HEALTH", "DATABASE_CONNECTIVITY", "COMMAND_PIPELINE", "WORKER_POOL", "SLO"};
            for (int i = 0; i < 40; i++) {
                String type = chkTypes[i % chkTypes.length] + "_" + (i / chkTypes.length);
                AbizDeploymentCheckpoint cp = govService.recordCheckpoint(rel.getId(), type, "EXPECTED", "EXPECTED", true, null);
                assertNotNull("34.4." + (i + 1) + " Checkpoint recorded with evidence hash", cp.getEvidenceHash());
            }

            // 5. Release Activation & Fail-Closed Gates (25 tests)
            boolean activated = govService.activateDeployment(rel.getId());
            assertTrue("34.5.1 Deployment activated after checkpoints passed", activated);

            for (int i = 2; i <= 25; i++) {
                AbizDeploymentRelease unapproved = govService.registerRelease("v22.3." + i, "BACKEND_WAR", "launcher.war", relSha, 42700000L, "operator_1", "PRODUCTION");
                boolean act = govService.activateDeployment(unapproved.getId());
                assertFalse("34.5." + i + " Unapproved/unverified release activation rejected", act);
            }

            // 6. Rollback Decisioning & Audit Evidence (25 tests)
            for (int i = 1; i <= 25; i++) {
                AbizDeploymentRelease toRollback = govService.registerRelease("v22.4." + i, "BACKEND_WAR", "launcher.war", relSha, 42700000L, "operator_1", "PRODUCTION");
                govService.approveRelease(toRollback.getId(), "lead_approver");
                govService.startDeployment(toRollback.getId());
                boolean rolledBack = govService.rollbackDeployment(toRollback.getId(), "SLO latency threshold breach simulation #" + i);
                assertTrue("34.6." + i + " Atomic rollback executed", rolledBack);
            }
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
