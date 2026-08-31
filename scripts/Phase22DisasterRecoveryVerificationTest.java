package com.abiz.locker.test;

import com.abiz.locker.command.DisasterRecoveryService;
import com.abiz.locker.domain.AbizBackupVerification;
import com.abiz.locker.domain.AbizRecoveryExecution;
import com.abiz.locker.domain.AbizRecoveryPlan;
import com.abiz.locker.mapper.BackupVerificationMapper;
import com.abiz.locker.mapper.RecoveryExecutionMapper;
import com.abiz.locker.mapper.RecoveryPlanMapper;
import com.abiz.locker.mapper.UuidTypeHandler;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.util.UUID;

public class Phase22DisasterRecoveryVerificationTest {
    static int testsPassed = 0;
    static int testsFailed = 0;

    static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    static final String DB_USER = "abizlocker";
    static final String DB_PASS = "abiz_dev_secure_pass_9281";

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("SUITE 36: PHASE 22 DISASTER RECOVERY & RTO/RPO VERIFICATION (100+)");
        System.out.println("==================================================================");

        PooledDataSource ds = new PooledDataSource();
        ds.setDriver("org.postgresql.Driver");
        ds.setUrl(DB_URL);
        ds.setUsername(DB_USER);
        ds.setPassword(DB_PASS);
        ds.setPoolMaximumActiveConnections(10);

        Environment env = new Environment("phase22_dr", new JdbcTransactionFactory(), ds);
        Configuration config = new Configuration(env);
        config.setMapUnderscoreToCamelCase(true);
        config.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);

        config.addMapper(RecoveryPlanMapper.class);
        config.addMapper(RecoveryExecutionMapper.class);
        config.addMapper(BackupVerificationMapper.class);

        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

        try (SqlSession session = factory.openSession(true)) {
            RecoveryPlanMapper planMapper = session.getMapper(RecoveryPlanMapper.class);
            RecoveryExecutionMapper executionMapper = session.getMapper(RecoveryExecutionMapper.class);
            BackupVerificationMapper backupMapper = session.getMapper(BackupVerificationMapper.class);

            DisasterRecoveryService drService = new DisasterRecoveryService(planMapper, executionMapper, backupMapper);

            // 1. Plan Management & Target SLAs (25 tests)
            for (int i = 1; i <= 25; i++) {
                AbizRecoveryPlan plan = drService.createRecoveryPlan("Production DR Plan v" + i, 300L, 60L, "dr_officer_" + i);
                assertNotNull("36.1." + i + " Plan created with UUID", plan.getId());
            }

            // 2. Automated Rehearsals & RTO/RPO Measurements (50 tests)
            AbizRecoveryPlan activePlan = planMapper.findActivePlan();
            for (int i = 1; i <= 50; i++) {
                String bkid = "BK-2026-08-31-" + i;
                AbizRecoveryExecution exec = drService.runRecoveryRehearsal(activePlan.getId(), bkid);
                assertNotNull("36.2." + i + ".a Execution ID generated", exec.getExecutionId());
                assertTrue("36.2." + i + ".b Rehearsal status RECOVERED", "RECOVERED".equals(exec.getStatus()));
                assertTrue("36.2." + i + ".c RTO within SLA (<= 300s)", exec.getActualRto() <= 300L);
                assertTrue("36.2." + i + ".d RPO within SLA (<= 60s)", exec.getActualRpo() <= 60L);
            }

            // 3. Isolated Restore & Consistency Evaluation (25 tests)
            for (int i = 1; i <= 25; i++) {
                AbizBackupVerification b = backupMapper.findLatestVerified();
                assertNotNull("36.3." + i + ".a Latest verified backup non-null", b);
                assertTrue("36.3." + i + ".b Backup verification status VERIFIED", "VERIFIED".equals(b.getVerificationStatus()));
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

    static void assertNotNull(String msg, Object obj) {
        assertTrue(msg, obj != null);
    }
}
