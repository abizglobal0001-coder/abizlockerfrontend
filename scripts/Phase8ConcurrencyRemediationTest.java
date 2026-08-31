package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.customer.CustomerService;
import com.abiz.locker.device.FinancedDeviceService;
import com.abiz.locker.domain.AbizFinancedDevice;
import com.abiz.locker.domain.AbizShop;
import com.abiz.locker.domain.DeviceRegisterRequest;
import com.abiz.locker.domain.ShopCreateRequest;
import com.abiz.locker.mapper.*;
import com.abiz.locker.shop.ShopService;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.postgresql.ds.PGSimpleDataSource;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Targeted Phase 8 Global IMEI Concurrency & Database Uniqueness Verification Suite.
 * Exercises live multi-threaded concurrent registrations against PostgreSQL 18.0.
 */
public class Phase8ConcurrencyRemediationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static long imeiSequence = 86000000000000L + (System.currentTimeMillis() % 10000000000L);

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 8 CONCURRENCY REMEDIATION VERIFICATION");
        System.out.println("===============================================================");

        try {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setServerNames(new String[]{"localhost"});
            ds.setPortNumbers(new int[]{3000});
            ds.setDatabaseName("abizlocker");
            ds.setUser("abizlocker");
            ds.setPassword("abiz_dev_secure_pass_9281");

            Environment env = new Environment("test", new JdbcTransactionFactory(), ds);
            Configuration config = new Configuration(env);
            config.getTypeHandlerRegistry().register(UUID.class, new UuidTypeHandler());
            config.addMapper(ShopMapper.class);
            config.addMapper(AuditMapper.class);
            config.addMapper(CustomerMapper.class);
            config.addMapper(FinancedDeviceMapper.class);

            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = factory.openSession(true)) {
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);
                CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);
                FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                Field auditMapperField = AbizAuditService.class.getDeclaredField("auditMapper");
                auditMapperField.setAccessible(true);
                auditMapperField.set(auditService, auditMapper);

                ShopService shopService = new ShopService(shopMapper, auditService);
                FinancedDeviceService deviceService = new FinancedDeviceService(deviceMapper, customerMapper, auditService);

                UUID adminId = UUID.randomUUID();
                AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "admin@abiz.com");

                AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                        "Conc Shop A " + UUID.randomUUID().toString().substring(0, 5),
                        "Owner A", "+923005551111", "concA@test.com", "Addr", "Lahore", "ACTIVE"
                ));
                AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                        "Conc Shop B " + UUID.randomUUID().toString().substring(0, 5),
                        "Owner B", "+923005552222", "concB@test.com", "Addr", "Karachi", "ACTIVE"
                ));
                UUID shopAId = shopA.getId();
                UUID shopBId = shopB.getId();
                AbizTenantContext.clear();

                // 1. Existing valid IMEI registration still succeeds
                System.out.println("\n--- Check 1: Valid Device Registration ---");
                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                String validImei = nextValidImei();
                DeviceRegisterRequest regReq = new DeviceRegisterRequest(validImei, "Redmi Note 13", "Xiaomi");
                AbizFinancedDevice dev1 = deviceService.registerDevice(regReq);
                assertCondition(dev1 != null && validImei.equals(dev1.getImei1()), "Valid IMEI registration succeeds");

                // 2. Duplicate IMEI 1 is rejected
                System.out.println("\n--- Check 2: Duplicate IMEI 1 Rejection ---");
                boolean dup1Rejected = false;
                try {
                    DeviceRegisterRequest dup1Req = new DeviceRegisterRequest(validImei, "Duplicate Model", "Xiaomi");
                    deviceService.registerDevice(dup1Req);
                } catch (IllegalArgumentException e) {
                    dup1Rejected = true;
                }
                assertCondition(dup1Rejected, "Duplicate IMEI 1 must be rejected");

                // 3. Duplicate IMEI 2 is rejected
                System.out.println("\n--- Check 3: Duplicate IMEI 2 Rejection ---");
                String imeiDual1 = nextValidImei();
                String imeiDual2 = nextValidImei();
                DeviceRegisterRequest dualReq = new DeviceRegisterRequest(imeiDual1, "Oppo Reno 10", "Oppo");
                dualReq.setImei2(imeiDual2);
                deviceService.registerDevice(dualReq);

                boolean dup2Rejected = false;
                try {
                    String otherImei = nextValidImei();
                    DeviceRegisterRequest dup2Req = new DeviceRegisterRequest(otherImei, "Other Model", "Oppo");
                    dup2Req.setImei2(imeiDual2);
                    deviceService.registerDevice(dup2Req);
                } catch (IllegalArgumentException e) {
                    dup2Rejected = true;
                }
                assertCondition(dup2Rejected, "Duplicate IMEI 2 must be rejected");

                // 4. IMEI 1 matching another device's IMEI 2 is rejected
                System.out.println("\n--- Check 4: Cross-SIM Collision Rejection ---");
                boolean crossSimRejected = false;
                try {
                    DeviceRegisterRequest crossSimReq = new DeviceRegisterRequest(imeiDual2, "Cross SIM", "Oppo");
                    deviceService.registerDevice(crossSimReq);
                } catch (IllegalArgumentException e) {
                    crossSimRejected = true;
                }
                assertCondition(crossSimRejected, "IMEI 1 matching another device's IMEI 2 must be rejected");

                // 5. Same physical IMEI across different shops is rejected
                System.out.println("\n--- Check 5: Global Cross-Shop Uniqueness ---");
                AbizTenantContext.clear();
                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
                boolean crossShopRejected = false;
                try {
                    DeviceRegisterRequest crossShopReq = new DeviceRegisterRequest(validImei, "Cross Shop", "Xiaomi");
                    deviceService.registerDevice(crossShopReq);
                } catch (IllegalArgumentException e) {
                    crossShopRejected = true;
                }
                assertCondition(crossShopRejected, "Same physical IMEI across different shops MUST be rejected");

                // 6. Different valid IMEIs succeed
                System.out.println("\n--- Check 6: Different Valid IMEIs Succeed ---");
                String imeiDistinct = nextValidImei();
                DeviceRegisterRequest distinctReq = new DeviceRegisterRequest(imeiDistinct, "Vivo V29", "Vivo");
                AbizFinancedDevice devB = deviceService.registerDevice(distinctReq);
                assertCondition(devB != null && imeiDistinct.equals(devB.getImei1()), "Different valid IMEI succeeds in Shop B");

                // 7. NULL IMEI 2 remains supported
                System.out.println("\n--- Check 7: NULL IMEI 2 Support ---");
                String imeiNull2 = nextValidImei();
                DeviceRegisterRequest null2Req = new DeviceRegisterRequest(imeiNull2, "Single SIM Phone", "Nokia");
                null2Req.setImei2(null);
                AbizFinancedDevice devNull2 = deviceService.registerDevice(null2Req);
                assertCondition(devNull2 != null && devNull2.getImei2() == null, "NULL IMEI 2 remains supported without constraint violation");

                // 8. DB Constraint: imei1 != imei2
                System.out.println("\n--- Check 8: DB Constraint imei1 != imei2 ---");
                boolean identicalRejected = false;
                try {
                    String identicalImei = nextValidImei();
                    DeviceRegisterRequest idReq = new DeviceRegisterRequest(identicalImei, "Identical Test", "Nokia");
                    idReq.setImei2(identicalImei);
                    deviceService.registerDevice(idReq);
                } catch (IllegalArgumentException e) {
                    identicalRejected = true;
                }
                assertCondition(identicalRejected, "Identical IMEI 1 and IMEI 2 rejected by validation and DB constraint");

                // 9. Concurrent Multi-Threaded Registration Race Condition Test
                System.out.println("\n--- Check 9: Concurrent Registration Race Condition (10 Threads) ---");
                final String concurrentTargetImei = nextValidImei();
                int numThreads = 10;
                ExecutorService executor = Executors.newFixedThreadPool(numThreads);
                CountDownLatch readyLatch = new CountDownLatch(numThreads);
                CountDownLatch startLatch = new CountDownLatch(1);
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger rejectedCount = new AtomicInteger(0);
                List<Future<?>> futures = new ArrayList<>();

                for (int i = 0; i < numThreads; i++) {
                    final int threadIndex = i;
                    futures.add(executor.submit(() -> {
                        readyLatch.countDown();
                        try {
                            startLatch.await(); // Simultaneous release
                            // Thread-local tenant context
                            UUID threadOwner = UUID.randomUUID();
                            UUID targetShop = (threadIndex % 2 == 0) ? shopAId : shopBId;
                            AbizTenantContext.set(threadOwner, AbizRole.SHOP_OWNER, targetShop, "conc" + threadIndex + "@test.com");

                            try (SqlSession threadSession = factory.openSession(true)) {
                                FinancedDeviceMapper threadDevMapper = threadSession.getMapper(FinancedDeviceMapper.class);
                                CustomerMapper threadCustMapper = threadSession.getMapper(CustomerMapper.class);
                                AuditMapper threadAuditMapper = threadSession.getMapper(AuditMapper.class);

                                AbizAuditService threadAuditService = new AbizAuditService();
                                Field amf = AbizAuditService.class.getDeclaredField("auditMapper");
                                amf.setAccessible(true);
                                amf.set(threadAuditService, threadAuditMapper);

                                FinancedDeviceService threadDeviceService = new FinancedDeviceService(threadDevMapper, threadCustMapper, threadAuditService);

                                DeviceRegisterRequest concReq = new DeviceRegisterRequest(
                                        concurrentTargetImei, "Concurrent Test " + threadIndex, "Tester"
                                );
                                threadDeviceService.registerDevice(concReq);
                                successCount.incrementAndGet();
                            }
                        } catch (IllegalArgumentException e) {
                            rejectedCount.incrementAndGet();
                        } catch (Exception e) {
                            System.err.println("Unexpected exception in thread " + threadIndex + ": " + e);
                            e.printStackTrace();
                        } finally {
                            AbizTenantContext.clear();
                        }
                    }));
                }

                readyLatch.await();
                startLatch.countDown(); // Fire all 10 threads at once!

                for (Future<?> f : futures) {
                    f.get(10, TimeUnit.SECONDS);
                }
                executor.shutdown();

                System.out.println(String.format("  [CONCURRENCY RESULT] 10 simultaneous threads: %d succeeded, %d rejected",
                        successCount.get(), rejectedCount.get()));

                assertCondition(successCount.get() == 1,
                        "Exactly ONE registration must succeed among concurrent race condition attempts");
                assertCondition(rejectedCount.get() == (numThreads - 1),
                        "All concurrent duplicate attempts must be cleanly rejected with IllegalArgumentException");

                // 10. Verify that at most ONE device record can exist for the same physical IMEI in PostgreSQL
                System.out.println("\n--- Check 10: PostgreSQL Absolute Row Count Verification ---");
                Connection conn = session.getConnection();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(1) FROM abiz_financed_devices WHERE imei1 = ? OR imei2 = ?")) {
                    ps.setString(1, concurrentTargetImei);
                    ps.setString(2, concurrentTargetImei);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int dbCount = rs.getInt(1);
                        assertCondition(dbCount == 1,
                                "Database contains EXACTLY 1 row for IMEI '" + concurrentTargetImei + "' (Found: " + dbCount + ")");
                    }
                }

                System.out.println("\n===============================================================");
                System.out.println(String.format("CONCURRENCY REMEDIATION RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
                System.out.println("===============================================================");

                if (testsFailed > 0) {
                    System.exit(1);
                }
            }

        } catch (Exception e) {
            System.err.println("FATAL CONCURRENCY TEST SUITE FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String nextValidImei() {
        imeiSequence++;
        String base = String.format("%014d", imeiSequence);
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int d = base.charAt(i) - '0';
            if (i % 2 != 0) {
                d *= 2;
                if (d > 9) d = (d % 10) + (d / 10);
            }
            sum += d;
        }
        int check = (10 - (sum % 10)) % 10;
        return base + check;
    }

    private static void assertCondition(boolean condition, String message) {
        if (condition) {
            System.out.println("  [PASS] " + message);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + message);
            testsFailed++;
        }
    }
}
