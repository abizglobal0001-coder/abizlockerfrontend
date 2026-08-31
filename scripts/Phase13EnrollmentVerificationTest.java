package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.contract.ContractService;
import com.abiz.locker.customer.CustomerService;
import com.abiz.locker.device.FinancedDeviceService;
import com.abiz.locker.domain.*;
import com.abiz.locker.enrollment.EnrollmentService;
import com.abiz.locker.installment.InstallmentService;
import com.abiz.locker.license.DeviceLicenseService;
import com.abiz.locker.mapper.*;
import com.abiz.locker.shop.ShopService;
import com.abiz.locker.wallet.WalletMapper;
import com.abiz.locker.wallet.WalletService;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.json.JSONObject;
import org.postgresql.ds.PGSimpleDataSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive verification test suite for ABIZ Locker Phase 13:
 * QR Enrollment & Android Device Provisioning.
 */
public class Phase13EnrollmentVerificationTest {

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
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 13 QR ENROLLMENT & PROVISIONING TEST");
        System.out.println("===============================================================");

        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[]{"localhost"});
        dataSource.setPortNumbers(new int[]{3000});
        dataSource.setDatabaseName("abizlocker");
        dataSource.setUser("abizlocker");
        dataSource.setPassword("abiz_dev_secure_pass_9281");

        Environment environment = new Environment("development", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.getTypeHandlerRegistry().register(UUID.class, new UuidTypeHandler());

        // Register Mappers
        configuration.addMapper(ShopMapper.class);
        configuration.addMapper(ShopUserMapper.class);
        configuration.addMapper(WalletMapper.class);
        configuration.addMapper(CustomerMapper.class);
        configuration.addMapper(FinancedDeviceMapper.class);
        configuration.addMapper(ContractMapper.class);
        configuration.addMapper(InstallmentMapper.class);
        configuration.addMapper(PaymentMapper.class);
        configuration.addMapper(DeviceLicenseMapper.class);
        configuration.addMapper(EnrollmentSessionMapper.class);
        configuration.addMapper(AuditMapper.class);

        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(configuration);

        try (SqlSession session = factory.openSession(true)) {
            ShopMapper shopMapper = session.getMapper(ShopMapper.class);
            ShopUserMapper shopUserMapper = session.getMapper(ShopUserMapper.class);
            WalletMapper walletMapper = session.getMapper(WalletMapper.class);
            CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);
            FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);
            ContractMapper contractMapper = session.getMapper(ContractMapper.class);
            InstallmentMapper installmentMapper = session.getMapper(InstallmentMapper.class);
            DeviceLicenseMapper licenseMapper = session.getMapper(DeviceLicenseMapper.class);
            EnrollmentSessionMapper enrollmentMapper = session.getMapper(EnrollmentSessionMapper.class);
            AuditMapper auditMapper = session.getMapper(AuditMapper.class);

            AbizAuditService auditService = new AbizAuditService();
            Field f = AbizAuditService.class.getDeclaredField("auditMapper");
            f.setAccessible(true);
            f.set(auditService, auditMapper);

            ShopService shopService = new ShopService(shopMapper, auditService);
            CustomerService customerService = new CustomerService(customerMapper, shopMapper, auditService);
            FinancedDeviceService deviceService = new FinancedDeviceService(deviceMapper, customerMapper, auditService);
            ContractService contractService = new ContractService(contractMapper, customerMapper, deviceMapper, auditService);
            EnrollmentService enrollmentService = new EnrollmentService(enrollmentMapper, contractMapper, customerMapper, licenseMapper, auditService);

            // 1. Seed Super Admin
            UUID superAdminId = UUID.randomUUID();
            AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "super13@abizlocker.com");

            // 2. Seed Shops
            AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                    "Enroll Shop A " + UUID.randomUUID().toString().substring(0, 5),
                    "Owner A", "+923001112233", "enrA_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Lahore", "Lahore", "ACTIVE"
            ));
            AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                    "Enroll Shop B " + UUID.randomUUID().toString().substring(0, 5),
                    "Owner B", "+923004445566", "enrB_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Karachi", "Karachi", "ACTIVE"
            ));
            UUID shopAId = shopA.getId();
            UUID shopBId = shopB.getId();

            // 3. Seed Users
            UUID ownerAId = UUID.randomUUID();
            UUID staffAId = UUID.randomUUID();
            UUID ownerBId = UUID.randomUUID();

            insertShopUser(shopUserMapper, ownerAId, shopAId, "Owner A", "ownera_" + ownerAId + "@test.com", "OWNER");
            insertShopUser(shopUserMapper, staffAId, shopAId, "Staff A", "staffa_" + staffAId + "@test.com", "STAFF");
            insertShopUser(shopUserMapper, ownerBId, shopBId, "Owner B", "ownerb_" + ownerBId + "@test.com", "OWNER");

            // 4. Seed Customers
            AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
            AbizCustomer custA = customerService.createCustomer(new CustomerCreateRequest(
                    "Customer 13A", "+923001119999", "35201-1119999-1", "Lahore"
            ));

            AbizTenantContext.set(ownerBId, AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
            AbizCustomer custB = customerService.createCustomer(new CustomerCreateRequest(
                    "Customer 13B", "+923002229999", "35201-2229999-1", "Karachi"
            ));

            // 5. Seed Devices & Contracts for Shop A
            AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
            AbizFinancedDevice devA1 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A14", "Samsung"));
            AbizFinancedDevice devA2 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A34", "Samsung"));
            AbizFinancedDevice devA3 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A54", "Samsung"));

            AbizContract contractA1 = createActiveContract(contractService, custA.getId(), devA1.getId(), new BigDecimal("30000.00"));
            AbizContract contractA2 = createActiveContract(contractService, custA.getId(), devA2.getId(), new BigDecimal("40000.00"));
            AbizContract contractA3 = createActiveContract(contractService, custA.getId(), devA3.getId(), new BigDecimal("50000.00"));

            // 6. Seed Device & Contract for Shop B
            AbizTenantContext.set(ownerBId, AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
            AbizFinancedDevice devB1 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Vivo Y27", "Vivo"));
            AbizContract contractB1 = createActiveContract(contractService, custB.getId(), devB1.getId(), new BigDecimal("25000.00"));

            AbizTenantContext.clear();

            // Run All Phase 13 Test Suites
            testSessionCreationAndQrGeneration(enrollmentService, deviceMapper, shopAId, ownerAId, devA1.getId(), contractA1.getId(), custA.getId());
            testProvisioningPayloadStructure(enrollmentService, shopAId, ownerAId, devA1.getId());
            testDeviceScanAndDetectionCallback(enrollmentService, shopAId, ownerAId, devA1.getId());
            String enrolledToken = testDeviceCompletionAndStateSync(enrollmentService, deviceMapper, shopAId, ownerAId, devA1.getId());
            testReplayAndOneTimeUseProtection(enrollmentService, enrolledToken);
            testSessionCancellation(enrollmentService, deviceMapper, shopAId, ownerAId, devA2.getId(), contractA2.getId(), custA.getId());
            testTtlExpirationHandling(factory, enrollmentService, shopAId, ownerAId, devA3.getId(), contractA3.getId(), custA.getId());
            testTripartiteTenantScopingAndRbac(enrollmentService, shopAId, shopBId, ownerAId, staffAId, superAdminId, devB1.getId(), contractB1.getId(), custB.getId());
            testConcurrent10ThreadsCompleteRace(factory, dataSource, enrollmentService, contractService, deviceService, customerService, shopAId, ownerAId, custA.getId());
            testAuditTrailLogging(dataSource);

            System.out.println("\n===============================================================");
            System.out.println(String.format("PHASE 13 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
            System.out.println("===============================================================");

            if (testsFailed > 0) {
                System.err.println("FATAL: " + testsFailed + " tests failed during Phase 13 verification.");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("FATAL PHASE 13 TEST SUITE FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testSessionCreationAndQrGeneration(
            EnrollmentService enrollmentService, FinancedDeviceMapper deviceMapper,
            UUID shopAId, UUID ownerAId, UUID devA1Id, UUID contractA1Id, UUID custAId) {
        System.out.println("\n--- Test Suite 1: Session Initiation & QR Code Generation ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            EnrollmentSessionRequest req = new EnrollmentSessionRequest(devA1Id, contractA1Id, custAId);
            EnrollmentSessionResponse resp = enrollmentService.createSession(req);

            assertCondition(resp != null && resp.getSession() != null, "Enrollment session created successfully");
            assertCondition("PENDING".equals(resp.getStatus()), "Initial session status is PENDING (Got: " + resp.getStatus() + ")");
            assertCondition(resp.getQrToken() != null && resp.getQrToken().length() >= 40, "Cryptographic 256-bit token generated (Length: " + resp.getQrToken().length() + ")");
            assertCondition(resp.getQrCodeBase64() != null && resp.getQrCodeBase64().startsWith("data:image/png;base64,"), "QR Code Base64 data URL generated");
            assertCondition(resp.getExpiresAt() != null && resp.getExpiresAt().after(new Date()), "Session expiresAt is in the future");

            // Verify raw PNG binary generation
            byte[] pngBytes = enrollmentService.generateQrPngBytes(resp.getSession().getId(), 350);
            assertCondition(pngBytes != null && pngBytes.length > 500, "Raw PNG QR bytes generated (Size: " + (pngBytes != null ? pngBytes.length : 0) + " bytes)");
            // Check PNG magic bytes (0x89, 'P', 'N', 'G')
            assertCondition(pngBytes[0] == (byte) 0x89 && pngBytes[1] == (byte) 'P' && pngBytes[2] == (byte) 'N' && pngBytes[3] == (byte) 'G',
                    "Valid PNG magic header verified (89 50 4E 47)");

            // Verify device table updated to PENDING
            AbizFinancedDevice dev = deviceMapper.findDeviceById(devA1Id);
            assertCondition("PENDING".equals(dev.getEnrollmentStatus()), "Device enrollment_status synchronized to PENDING in database");

            // Idempotent duplicate call returns existing active session
            EnrollmentSessionResponse duplicateResp = enrollmentService.createSession(req);
            assertCondition(resp.getSession().getId().equals(duplicateResp.getSession().getId()), "Duplicate session request returned existing active session (Idempotent)");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testProvisioningPayloadStructure(
            EnrollmentService enrollmentService, UUID shopAId, UUID ownerAId, UUID devA1Id) {
        System.out.println("\n--- Test Suite 2: Android Enterprise Provisioning Payload Structure ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            EnrollmentSessionRequest req = new EnrollmentSessionRequest();
            req.setDeviceId(devA1Id);
            EnrollmentSessionResponse resp = enrollmentService.createSession(req);

            String jsonStr = resp.getProvisioningPayloadJson();
            assertCondition(jsonStr != null && !jsonStr.trim().isEmpty(), "Provisioning JSON string is populated");

            JSONObject json = new JSONObject(jsonStr);
            assertCondition(json.has("android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"), "Payload contains admin component name");
            assertCondition(json.has("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"), "Payload contains APK download location");
            assertCondition(json.has("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM"), "Payload contains package checksum");
            assertCondition(json.getBoolean("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED"), "Leave all system apps enabled is true");
            assertCondition(json.getBoolean("android.app.extra.PROVISIONING_SKIP_ENCRYPTION"), "Skip encryption extra is true");

            JSONObject extras = json.getJSONObject("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE");
            assertCondition(extras != null, "Admin extras bundle is present");
            assertCondition(resp.getQrToken().equals(extras.getString("qrToken")), "Extras bundle contains matching qrToken");
            assertCondition(devA1Id.toString().equals(extras.getString("deviceId")), "Extras bundle contains matching deviceId");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testDeviceScanAndDetectionCallback(
            EnrollmentService enrollmentService, UUID shopAId, UUID ownerAId, UUID devA1Id) {
        System.out.println("\n--- Test Suite 3: Device Scan & Detection Callback (/detect) ---");

        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        final String qrToken;
        try {
            EnrollmentSessionRequest req = new EnrollmentSessionRequest();
            req.setDeviceId(devA1Id);
            EnrollmentSessionResponse resp = enrollmentService.createSession(req);
            qrToken = resp.getQrToken();
        } finally {
            AbizTenantContext.clear();
        }

        // Public callback without tenant context (Device scanning QR)
        AbizEnrollmentSession session = enrollmentService.detectDevice(qrToken);
        assertCondition(session != null, "Public /detect callback processed");
        assertCondition("DETECTED".equals(session.getStatus()), "Session status transitioned to DETECTED (Got: " + session.getStatus() + ")");

        // Reject invalid token
        boolean invalidTokenBlocked = false;
        try {
            enrollmentService.detectDevice("INVALID-TOKEN-9999");
        } catch (IllegalArgumentException e) {
            invalidTokenBlocked = true;
        }
        assertCondition(invalidTokenBlocked, "Invalid token rejected with IllegalArgumentException");
    }

    private static String testDeviceCompletionAndStateSync(
            EnrollmentService enrollmentService, FinancedDeviceMapper deviceMapper,
            UUID shopAId, UUID ownerAId, UUID devA1Id) {
        System.out.println("\n--- Test Suite 4: Device Completion Callback & State Synchronization ---");

        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        final String qrToken;
        try {
            EnrollmentSessionRequest req = new EnrollmentSessionRequest();
            req.setDeviceId(devA1Id);
            EnrollmentSessionResponse resp = enrollmentService.createSession(req);
            qrToken = resp.getQrToken();
        } finally {
            AbizTenantContext.clear();
        }

        DeviceEnrollmentCompletionRequest compReq = new DeviceEnrollmentCompletionRequest();
        compReq.setToken(qrToken);
        compReq.setMdmDeviceNumber("MDM-DEV-" + UUID.randomUUID().toString().substring(0, 8));
        compReq.setAndroidVersion("14");
        compReq.setAgentVersion("2.1.0");
        compReq.setSerialNumber("SN-" + UUID.randomUUID().toString().substring(0, 6));
        compReq.setModel("Galaxy A14");
        compReq.setManufacturer("Samsung");

        AbizEnrollmentSession enrolledSession = enrollmentService.completeEnrollment(compReq);
        assertCondition("ENROLLED".equals(enrolledSession.getStatus()), "Session status transitioned to ENROLLED");
        assertCondition(enrolledSession.getCompletedAt() != null, "Session completed_at timestamp is set");

        // Verify live database state in abiz_financed_devices
        AbizFinancedDevice dev = deviceMapper.findDeviceById(devA1Id);
        assertCondition("ENROLLED".equals(dev.getEnrollmentStatus()), "Device enrollment_status is ENROLLED in database");
        assertCondition(dev.isDeviceOwnerActive(), "Device device_owner_active is TRUE");
        assertCondition("ONLINE".equals(dev.getConnectionStatus()), "Device connection_status is ONLINE");
        assertCondition(dev.getLastSeenAt() != null, "Device last_seen_at timestamp updated");
        assertCondition(compReq.getMdmDeviceNumber().equals(dev.getMdmDeviceNumber()), "Device mdm_device_number synchronized");

        return qrToken;
    }

    private static void testReplayAndOneTimeUseProtection(
            EnrollmentService enrollmentService, String enrolledQrToken) {
        System.out.println("\n--- Test Suite 5: Replay & One-Time-Use Token Protection ---");

        // Replay attempt on already completed session
        boolean replayBlocked = false;
        try {
            DeviceEnrollmentCompletionRequest compReq = new DeviceEnrollmentCompletionRequest();
            compReq.setToken(enrolledQrToken);
            compReq.setMdmDeviceNumber("MDM-REPLAY");
            enrollmentService.completeEnrollment(compReq);
        } catch (IllegalStateException e) {
            replayBlocked = true;
        }

        assertCondition(replayBlocked, "Replay attack on ENROLLED token MUST be rejected with IllegalStateException");
    }

    private static void testSessionCancellation(
            EnrollmentService enrollmentService, FinancedDeviceMapper deviceMapper,
            UUID shopAId, UUID ownerAId, UUID devA2Id, UUID contractA2Id, UUID custAId) {
        System.out.println("\n--- Test Suite 6: Session Cancellation ---");

        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            EnrollmentSessionRequest req = new EnrollmentSessionRequest(devA2Id, contractA2Id, custAId);
            EnrollmentSessionResponse resp = enrollmentService.createSession(req);
            UUID sessionId = resp.getSession().getId();
            String qrToken = resp.getQrToken();

            AbizEnrollmentSession cancelled = enrollmentService.cancelSession(sessionId, "Customer opted out");
            assertCondition("CANCELLED".equals(cancelled.getStatus()), "Session status transitioned to CANCELLED");

            // Device enrollment status reset
            AbizFinancedDevice dev = deviceMapper.findDeviceById(devA2Id);
            assertCondition("NOT_ENROLLED".equals(dev.getEnrollmentStatus()), "Device enrollment_status reset to NOT_ENROLLED");

            // Attempting completion on cancelled token must fail
            boolean cancelledBlocked = false;
            try {
                DeviceEnrollmentCompletionRequest compReq = new DeviceEnrollmentCompletionRequest();
                compReq.setToken(qrToken);
                enrollmentService.completeEnrollment(compReq);
            } catch (IllegalStateException e) {
                cancelledBlocked = true;
            }
            assertCondition(cancelledBlocked, "Completion attempt on CANCELLED session rejected with IllegalStateException");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testTtlExpirationHandling(
            SqlSessionFactory factory, EnrollmentService enrollmentService,
            UUID shopAId, UUID ownerAId, UUID devA3Id, UUID contractA3Id, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 7: TTL Expiration Handling ---");

        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        final UUID sessionId;
        final String qrToken;
        try {
            EnrollmentSessionRequest req = new EnrollmentSessionRequest(devA3Id, contractA3Id, custAId);
            EnrollmentSessionResponse resp = enrollmentService.createSession(req);
            sessionId = resp.getSession().getId();
            qrToken = resp.getQrToken();
        } finally {
            AbizTenantContext.clear();
        }

        // Manually backdate expiration timestamp in database to simulate expiry
        try (SqlSession updateSession = factory.openSession(true)) {
            Connection conn = updateSession.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE abiz_enrollment_sessions SET expires_at = NOW() - INTERVAL '1 minute' WHERE id = ?")) {
                ps.setObject(1, sessionId);
                ps.executeUpdate();
            }
        }

        // Device attempt to detect or complete expired session
        boolean detectExpiredBlocked = false;
        try {
            enrollmentService.detectDevice(qrToken);
        } catch (IllegalStateException e) {
            detectExpiredBlocked = true;
        }
        assertCondition(detectExpiredBlocked, "Expired session /detect attempt rejected with IllegalStateException");

        boolean completeExpiredBlocked = false;
        try {
            DeviceEnrollmentCompletionRequest compReq = new DeviceEnrollmentCompletionRequest();
            compReq.setToken(qrToken);
            enrollmentService.completeEnrollment(compReq);
        } catch (IllegalStateException e) {
            completeExpiredBlocked = true;
        }
        assertCondition(completeExpiredBlocked, "Expired session /complete attempt rejected with IllegalStateException");
    }

    private static void testTripartiteTenantScopingAndRbac(
            EnrollmentService enrollmentService, UUID shopAId, UUID shopBId,
            UUID ownerAId, UUID staffAId, UUID superAdminId,
            UUID devB1Id, UUID contractB1Id, UUID custBId) {
        System.out.println("\n--- Test Suite 8: Tripartite Tenant Scoping & RBAC ---");

        // 1. Shop Owner A attempting to enroll Shop B device
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        boolean ownerCrossBlocked = false;
        try {
            enrollmentService.createSession(new EnrollmentSessionRequest(devB1Id, contractB1Id, custBId));
        } catch (SecurityException e) {
            ownerCrossBlocked = true;
        } finally {
            AbizTenantContext.clear();
        }
        assertCondition(ownerCrossBlocked, "Shop Owner A MUST be blocked from initiating enrollment for Shop B device (403 Forbidden)");

        // 2. Shop Staff A attempting to enroll Shop B device
        AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopAId, "staff.a@test.com");
        boolean staffCrossBlocked = false;
        try {
            enrollmentService.createSession(new EnrollmentSessionRequest(devB1Id, contractB1Id, custBId));
        } catch (SecurityException e) {
            staffCrossBlocked = true;
        } finally {
            AbizTenantContext.clear();
        }
        assertCondition(staffCrossBlocked, "Shop Staff A MUST be blocked from initiating enrollment for Shop B device (403 Forbidden)");

        // 3. Super Admin global list oversight
        AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "super@test.com");
        try {
            List<AbizEnrollmentSession> allSessions = enrollmentService.listSessions(null, null, 0, 50);
            assertCondition(allSessions.size() >= 3, "SUPER_ADMIN can list enrollment sessions across all shops (Found: " + allSessions.size() + ")");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testConcurrent10ThreadsCompleteRace(
            SqlSessionFactory factory, PGSimpleDataSource dataSource,
            EnrollmentService enrollmentService, ContractService contractService,
            FinancedDeviceService deviceService, CustomerService customerService,
            UUID shopAId, UUID ownerAId, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 9: Multi-Threaded Concurrency (10 Threads Racing to Complete 1 Session) ---");

        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        final UUID targetDevId;
        final String targetQrToken;
        try {
            AbizFinancedDevice devRace = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Race Model", "Samsung"));
            targetDevId = devRace.getId();
            AbizContract contractRace = createActiveContract(contractService, custAId, targetDevId, new BigDecimal("20000.00"));
            EnrollmentSessionResponse resp = enrollmentService.createSession(new EnrollmentSessionRequest(targetDevId, contractRace.getId(), custAId));
            targetQrToken = resp.getQrToken();
        } finally {
            AbizTenantContext.clear();
        }

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int tIndex = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronized start

                    try (SqlSession threadSession = factory.openSession(false)) {
                        EnrollmentSessionMapper threadEnrollMapper = threadSession.getMapper(EnrollmentSessionMapper.class);
                        ContractMapper threadContractMapper = threadSession.getMapper(ContractMapper.class);
                        CustomerMapper threadCustMapper = threadSession.getMapper(CustomerMapper.class);
                        DeviceLicenseMapper threadLicMapper = threadSession.getMapper(DeviceLicenseMapper.class);
                        AuditMapper threadAuditMapper = threadSession.getMapper(AuditMapper.class);

                        AbizAuditService threadAudit = new AbizAuditService();
                        Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                        f.setAccessible(true);
                        f.set(threadAudit, threadAuditMapper);

                        EnrollmentService threadService = new EnrollmentService(
                                threadEnrollMapper, threadContractMapper, threadCustMapper, threadLicMapper, threadAudit
                        );

                        DeviceEnrollmentCompletionRequest compReq = new DeviceEnrollmentCompletionRequest();
                        compReq.setToken(targetQrToken);
                        compReq.setMdmDeviceNumber("MDM-CONC-" + tIndex);
                        compReq.setModel("Race Model");
                        compReq.setManufacturer("Samsung");

                        threadService.completeEnrollment(compReq);
                        threadSession.commit();
                        successCount.incrementAndGet();
                    }
                } catch (IllegalStateException | IllegalArgumentException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Thread " + tIndex + " unexpected error: " + e.getMessage());
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // FIRE ALL 10 THREADS SIMULTANEOUSLY

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        System.out.println(String.format("  [CONCURRENCY METRICS] 10 concurrent threads: %d succeeded, %d cleanly rejected (already enrolled)",
                successCount.get(), rejectedCount.get()));

        assertCondition(successCount.get() == 1, "EXACTLY 1 thread succeeded in completing the session (Found: " + successCount.get() + ")");
        assertCondition(rejectedCount.get() == 9, "EXACTLY 9 threads were safely rejected as already enrolled (Found: " + rejectedCount.get() + ")");

        // Direct independent database check
        try (Connection conn = dataSource.getConnection();
             PreparedStatement psDev = conn.prepareStatement("SELECT enrollment_status, device_owner_active, connection_status FROM abiz_financed_devices WHERE id = ?");
             PreparedStatement psSess = conn.prepareStatement("SELECT status, completed_at FROM abiz_enrollment_sessions WHERE qr_token = ?")
        ) {
            psDev.setObject(1, targetDevId);
            try (ResultSet rs = psDev.executeQuery()) {
                rs.next();
                assertCondition("ENROLLED".equals(rs.getString("enrollment_status")), "PostgreSQL device enrollment_status is ENROLLED");
                assertCondition(rs.getBoolean("device_owner_active"), "PostgreSQL device device_owner_active is TRUE");
                assertCondition("ONLINE".equals(rs.getString("connection_status")), "PostgreSQL device connection_status is ONLINE");
            }

            psSess.setString(1, targetQrToken);
            try (ResultSet rs = psSess.executeQuery()) {
                rs.next();
                assertCondition("ENROLLED".equals(rs.getString("status")), "PostgreSQL session status is ENROLLED");
                assertCondition(rs.getTimestamp("completed_at") != null, "PostgreSQL session completed_at is set");
            }
        }
    }

    private static void testAuditTrailLogging(PGSimpleDataSource dataSource) {
        System.out.println("\n--- Test Suite 10: Audit Trail Generation for Enrollment Events ---");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT action, result, COUNT(*) AS cnt FROM abiz_audit_logs " +
                             "WHERE action IN ('ENROLLMENT_SESSION_CREATED', 'DEVICE_ENROLLED', 'ENROLLMENT_CANCELLED', 'ENROLLMENT_FAILED', 'CROSS_TENANT_ENROLLMENT_ACCESS_BLOCKED') " +
                             "GROUP BY action, result ORDER BY action")
        ) {
            try (ResultSet rs = ps.executeQuery()) {
                int totalLogs = 0;
                while (rs.next()) {
                    String action = rs.getString("action");
                    String result = rs.getString("result");
                    int count = rs.getInt("cnt");
                    System.out.println(String.format("  [AUDIT LOGGED] action=%s, result=%s, count=%d", action, result, count));
                    totalLogs += count;
                }
                assertCondition(totalLogs >= 4, "Enrollment operational events recorded in abiz_audit_logs (Total: " + totalLogs + ")");
            }
        } catch (Exception e) {
            testsFailed++;
            System.err.println("Audit trail verification failed: " + e.getMessage());
        }
    }

    private static AbizContract createActiveContract(ContractService contractService, UUID custId, UUID devId, BigDecimal price) {
        ContractCreateRequest req = new ContractCreateRequest();
        req.setCustomerId(custId);
        req.setFinancedDeviceId(devId);
        req.setDevicePrice(price);
        req.setDownPayment(BigDecimal.ZERO);
        req.setFinancedAmount(price);
        req.setInstallmentAmount(price.divide(new BigDecimal("2")));
        req.setNumberOfInstallments(2);
        req.setFirstDueDate(new Date());
        req.setFrequency("MONTHLY");

        AbizContract contract = contractService.createContract(req);
        contractService.activateContract(contract.getId());
        return contract;
    }

    private static void insertShopUser(ShopUserMapper mapper, UUID id, UUID shopId, String name, String email, String role) {
        AbizShopUser u = new AbizShopUser();
        u.setId(id);
        u.setShopId(shopId);
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setRole(role);
        u.setStatus("ACTIVE");
        mapper.insertShopUser(u);
    }

    private static void assertCondition(boolean condition, String description) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + description);
            testsFailed++;
        }
    }
}
