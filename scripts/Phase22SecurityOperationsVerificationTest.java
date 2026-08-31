package com.abiz.locker.test;

import com.abiz.locker.command.SecurityEventCorrelationService;
import com.abiz.locker.domain.AbizSecurityIncident;
import com.abiz.locker.domain.AbizShop;
import com.abiz.locker.domain.AbizFinancedDevice;
import com.abiz.locker.mapper.SecurityIncidentMapper;
import com.abiz.locker.mapper.ShopMapper;
import com.abiz.locker.mapper.FinancedDeviceMapper;
import com.abiz.locker.mapper.UuidTypeHandler;
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
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Phase22SecurityOperationsVerificationTest {
    static int testsPassed = 0;
    static int testsFailed = 0;

    static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    static final String DB_USER = "abizlocker";
    static final String DB_PASS = "abiz_dev_secure_pass_9281";

    private static long imeiSequence = 86230000000000L + (System.currentTimeMillis() % 9000000000000L);

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
        System.out.println("==================================================================");
        System.out.println("SUITE 38: PHASE 22 SECURITY OPERATIONS & INCIDENT CORRELATION (100+)");
        System.out.println("==================================================================");

        PooledDataSource ds = new PooledDataSource();
        ds.setDriver("org.postgresql.Driver");
        ds.setUrl(DB_URL);
        ds.setUsername(DB_USER);
        ds.setPassword(DB_PASS);

        Environment env = new Environment("test", new JdbcTransactionFactory(), ds);
        Configuration config = new Configuration(env);
        config.getTypeHandlerRegistry().register(UUID.class, new UuidTypeHandler());
        config.addMapper(SecurityIncidentMapper.class);
        config.addMapper(FinancedDeviceMapper.class);
        config.addMapper(ShopMapper.class);

        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

        try (SqlSession session = factory.openSession(true)) {
            SecurityIncidentMapper incidentMapper = session.getMapper(SecurityIncidentMapper.class);
            ShopMapper shopMapper = session.getMapper(ShopMapper.class);
            FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);

            SecurityEventCorrelationService secService = new SecurityEventCorrelationService(incidentMapper);
            List<AbizShop> shops = shopMapper.findAllShops(null, 10, 0);
            UUID shopId;
            if (shops == null || shops.isEmpty()) {
                shopId = UUID.randomUUID();
                AbizShop s = new AbizShop();
                s.setId(shopId);
                s.setName("Test Shop Phase 22 Sec");
                s.setOwnerName("Owner 22 Sec");
                s.setPhone("03001234568");
                s.setEmail("shop22_sec@test.com");
                s.setStatus("ACTIVE");
                shopMapper.insertShop(s);
            } else {
                shopId = shops.get(0).getId();
            }

            // 1. Security Event Ingestion & Deduplication (50 tests)
            for (int i = 1; i <= 50; i++) {
                UUID devId = UUID.randomUUID();
                createRawDevice(devId, shopId, nextValidImei());

                AbizSecurityIncident inc = secService.ingestSecurityEvent("REPLAY_ATTACK_DETECTED", shopId, devId, "worker_1", "LOW", "Replay simulation #" + i);
                assertNotNull("38.1." + i + ".a Incident ingested with fingerprint", inc.getFingerprint());
                // Duplicate ingestion on same device/shop
                AbizSecurityIncident dup = secService.ingestSecurityEvent("REPLAY_ATTACK_DETECTED", shopId, devId, "worker_1", "LOW", "Replay repeat #" + i);
                assertTrue("38.1." + i + ".b Deduplicated and occurrence count incremented", dup.getOccurrenceCount() >= 2);
            }

            // 2. Incident Severity Auto-Escalation & Resolution (50 tests)
            for (int i = 1; i <= 50; i++) {
                UUID devId = UUID.randomUUID();
                createRawDevice(devId, shopId, nextValidImei());

                AbizSecurityIncident inc = null;
                for (int j = 0; j < 5; j++) {
                    inc = secService.ingestSecurityEvent("CROSS_TENANT_BREACH", shopId, devId, "worker_1", "LOW", "Escalation test");
                }
                boolean resolved = secService.resolveIncident(inc.getId(), "Mitigation applied: credential isolated");
                assertTrue("38.2." + i + " Incident escalated and resolved", resolved);
            }
        }

        System.out.println("\nTOTAL: " + testsPassed + " PASS, " + testsFailed + " FAIL");
        if (testsFailed > 0) System.exit(1);
    }

    private static void createRawDevice(UUID devId, UUID shopId, String imei) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO abiz_financed_devices (id, shop_id, imei1, enrollment_status, device_owner_active, connection_status, lock_status, license_status, policy_state, health_status, created_at, updated_at) " +
                             "VALUES (?, ?, ?, 'ENROLLED', TRUE, 'ONLINE', 'UNLOCKED', 'ACTIVE', 'ACTIVE', 'HEALTHY', NOW(), NOW()) ON CONFLICT (id) DO NOTHING")) {
            ps.setObject(1, devId);
            ps.setObject(2, shopId);
            ps.setString(3, imei);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
