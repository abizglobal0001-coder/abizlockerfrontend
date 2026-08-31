package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.domain.*;
import com.abiz.locker.license.DeviceLicenseService;
import com.abiz.locker.mapper.*;
import com.abiz.locker.wallet.WalletMapper;
import com.abiz.locker.wallet.WalletService;
import com.abiz.locker.wallet.WalletService.InsufficientCreditsException;
import com.abiz.locker.wallet.WalletService.WalletBalance;
import com.abiz.locker.wallet.WalletService.WalletRow;
import com.abiz.locker.wallet.WalletMapper.WalletTransactionRow;
import org.apache.ibatis.mapping.Environment;
import org.postgresql.ds.PGSimpleDataSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive verification test suite for ABIZ Locker Phase 12:
 * Device Licensing & Wallet Credit Consumption.
 */
public class Phase12LicenseVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static final String DB_URL = "jdbc:postgresql://127.0.0.1:3000/abizlocker";
    private static final String DB_USER = "abizlocker";
    private static final String DB_PASS = "abiz_dev_secure_pass_9281";

    private static long imeiSequence = 860000000000000L + (System.currentTimeMillis() % 90000000000000L);

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
        System.out.println("ABIZ LOCKER — PHASE 12 DEVICE LICENSING & WALLET CONSUMPTION TEST");
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

        // Register All Mappers
        configuration.addMapper(ShopMapper.class);
        configuration.addMapper(ShopUserMapper.class);
        configuration.addMapper(WalletMapper.class);
        configuration.addMapper(CustomerMapper.class);
        configuration.addMapper(FinancedDeviceMapper.class);
        configuration.addMapper(ContractMapper.class);
        configuration.addMapper(InstallmentMapper.class);
        configuration.addMapper(PaymentMapper.class);
        configuration.addMapper(DeviceLicenseMapper.class);
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
            AuditMapper auditMapper = session.getMapper(AuditMapper.class);

            AbizAuditService auditService = new AbizAuditService();
            Field f = AbizAuditService.class.getDeclaredField("auditMapper");
            f.setAccessible(true);
            f.set(auditService, auditMapper);

            com.abiz.locker.shop.ShopService shopService = new com.abiz.locker.shop.ShopService(shopMapper, auditService);
            WalletService walletService = new WalletService(walletMapper, auditService);
            DeviceLicenseService licenseService = new DeviceLicenseService(licenseMapper, walletMapper, auditService);

            // 1. Seed Super Admin Context
            UUID superAdminId = UUID.randomUUID();
            AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "super12@abizlocker.com");

            // 2. Seed Shops
            AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                    "License Shop A " + UUID.randomUUID().toString().substring(0, 5),
                    "Owner A", "+923001112233", "licA_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Lahore", "Lahore", "ACTIVE"
            ));
            AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                    "License Shop B " + UUID.randomUUID().toString().substring(0, 5),
                    "Owner B", "+923004445566", "licB_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Karachi", "Karachi", "ACTIVE"
            ));
            UUID shopAId = shopA.getId();
            UUID shopBId = shopB.getId();

            // 3. Seed Shop Users
            UUID ownerAId = UUID.randomUUID();
            UUID staffAId = UUID.randomUUID();
            UUID ownerBId = UUID.randomUUID();

            AbizShopUser userOwnerA = new AbizShopUser();
            userOwnerA.setId(ownerAId);
            userOwnerA.setShopId(shopAId);
            userOwnerA.setName("Owner A");
            userOwnerA.setEmail("ownera_" + ownerAId + "@test.com");
            userOwnerA.setPasswordHash("hash");
            userOwnerA.setRole("OWNER");
            userOwnerA.setStatus("ACTIVE");

            AbizShopUser userStaffA = new AbizShopUser();
            userStaffA.setId(staffAId);
            userStaffA.setShopId(shopAId);
            userStaffA.setName("Staff A");
            userStaffA.setEmail("staffa_" + staffAId + "@test.com");
            userStaffA.setPasswordHash("hash");
            userStaffA.setRole("STAFF");
            userStaffA.setStatus("ACTIVE");

            AbizShopUser userOwnerB = new AbizShopUser();
            userOwnerB.setId(ownerBId);
            userOwnerB.setShopId(shopBId);
            userOwnerB.setName("Owner B");
            userOwnerB.setEmail("ownerb_" + ownerBId + "@test.com");
            userOwnerB.setPasswordHash("hash");
            userOwnerB.setRole("OWNER");
            userOwnerB.setStatus("ACTIVE");

            shopUserMapper.insertShopUser(userOwnerA);
            shopUserMapper.insertShopUser(userStaffA);
            shopUserMapper.insertShopUser(userOwnerB);

            // 4. Seed Wallets (via Super Admin)
            AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "super@test.com");
            walletService.createWallet(shopAId);
            walletService.createWallet(shopBId);
            walletService.addCredits(shopAId, 10, "Initial test credits for Shop A");
            walletService.addCredits(shopBId, 5, "Initial test credits for Shop B");
            AbizTenantContext.clear();

            // 5. Seed Devices
            UUID devA1Id = UUID.randomUUID();
            UUID devA2Id = UUID.randomUUID();
            UUID devA3Id = UUID.randomUUID();
            UUID devB1Id = UUID.randomUUID();

            insertDevice(deviceMapper, devA1Id, shopAId, nextValidImei(), "Galaxy A14", "Samsung");
            insertDevice(deviceMapper, devA2Id, shopAId, nextValidImei(), "Galaxy A34", "Samsung");
            insertDevice(deviceMapper, devA3Id, shopAId, nextValidImei(), "Galaxy A54", "Samsung");
            insertDevice(deviceMapper, devB1Id, shopBId, nextValidImei(), "Vivo Y27", "Vivo");

            // Execute Test Suites
            testLicenseActivationAndCreditDeduction(licenseService, walletService, deviceMapper, shopAId, ownerAId, devA1Id);
            testInsufficientCreditsRejection(licenseService, walletService, deviceMapper, shopBId, ownerBId);
            testIdempotentDuplicateActivation(licenseService, walletService, shopAId, ownerAId, devA1Id);
            testLicenseDurationAndExpiration(licenseService, shopAId, ownerAId, devA2Id);
            testLicenseRevocationAndStateSync(licenseService, deviceMapper, shopAId, ownerAId, superAdminId, devA2Id);
            testRbacEnforcement(licenseService, walletService, shopAId, staffAId, ownerAId, superAdminId, devA3Id);
            testCrossTenantSecurity(licenseService, shopAId, shopBId, ownerAId, staffAId, superAdminId, devB1Id);
            testConcurrent10ThreadsOneCreditRace(factory, dataSource, deviceMapper, shopAId, ownerAId);
            testAuditTrailLogging(dataSource);

            System.out.println("\n===============================================================");
            System.out.println(String.format("PHASE 12 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
            System.out.println("===============================================================");

            if (testsFailed > 0) {
                System.err.println("FATAL: " + testsFailed + " tests failed during Phase 12 verification.");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("FATAL PHASE 12 TEST SUITE FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testLicenseActivationAndCreditDeduction(
            DeviceLicenseService licenseService, WalletService walletService,
            FinancedDeviceMapper deviceMapper, UUID shopAId, UUID ownerAId, UUID devA1Id) {
        System.out.println("\n--- Test Suite 1: License Activation & Wallet Credit Deduction ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            int balanceBefore = walletService.getBalance(shopAId).getBalance();
            assertCondition(balanceBefore == 10, "Initial Shop A wallet balance is 10 credits");

            LicenseActivateRequest req = new LicenseActivateRequest(devA1Id, 365, 1, "Initial 1-year license");
            DeviceLicenseResponse resp = licenseService.activateLicense(req);

            assertCondition(resp != null && resp.getLicense() != null, "License activation response returned");
            assertCondition("ACTIVE".equals(resp.getStatus()), "License status is ACTIVE (Got: " + resp.getStatus() + ")");
            assertCondition(resp.getWalletBalance() == 9, "Returned wallet balance is 9 (Got: " + resp.getWalletBalance() + ")");
            assertCondition(resp.getDaysRemaining() >= 364, "Days remaining is ~365 (Got: " + resp.getDaysRemaining() + ")");

            // Verify live wallet balance
            int balanceAfter = walletService.getBalance(shopAId).getBalance();
            assertCondition(balanceAfter == 9, "Live wallet balance decremented by exactly 1 to 9 (Got: " + balanceAfter + ")");

            // Verify device license status in database
            AbizFinancedDevice dev = deviceMapper.findDeviceById(devA1Id);
            assertCondition("ACTIVE".equals(dev.getLicenseStatus()), "Device license_status synchronized to ACTIVE in database");

            // Verify transaction ledger entry in wallet transactions
            List<WalletTransactionRow> txs = walletService.listTransactions(shopAId, 0, 10);
            WalletTransactionRow latestTx = txs.get(0);
            assertCondition("DEVICE_ACTIVATION_CONFIRM".equals(latestTx.type()), "Ledger transaction type is DEVICE_ACTIVATION_CONFIRM");
            assertCondition(latestTx.amount() == -1, "Ledger transaction amount is -1 credit");
            assertCondition(latestTx.balanceBefore() == 10 && latestTx.balanceAfter() == 9, "Ledger balance_before=10, balance_after=9");
            assertCondition(resp.getLicense().getId().equals(latestTx.relatedId()), "Ledger transaction related_id matches licenseId");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testInsufficientCreditsRejection(
            DeviceLicenseService licenseService, WalletService walletService,
            FinancedDeviceMapper deviceMapper, UUID shopBId, UUID ownerBId) {
        System.out.println("\n--- Test Suite 2: Insufficient Credits Protection ---");

        // Drain Shop B wallet to 0 credits using direct reserve
        AbizTenantContext.set(ownerBId, AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
        try {
            int currentBal = walletService.getBalance(shopBId).getBalance();
            if (currentBal > 0) {
                walletService.reserveActivationCredit(shopBId, UUID.randomUUID(), currentBal);
            }
            int zeroBal = walletService.getBalance(shopBId).getBalance();
            assertCondition(zeroBal == 0, "Shop B wallet balance is drained to 0 credits");

            UUID devZero = UUID.randomUUID();
            insertDevice(deviceMapper, devZero, shopBId, nextValidImei(), "Zero Bal Model", "Infinix");

            boolean rejected = false;
            try {
                licenseService.activateLicense(new LicenseActivateRequest(devZero, 365, 1, "Should fail"));
            } catch (InsufficientCreditsException e) {
                rejected = true;
            }

            assertCondition(rejected, "License activation with 0 credits MUST throw InsufficientCreditsException");

            // Verify state untouched
            int balAfter = walletService.getBalance(shopBId).getBalance();
            assertCondition(balAfter == 0, "Wallet balance remains 0 (no negative overdraft!)");

            AbizFinancedDevice devAfter = deviceMapper.findDeviceById(devZero);
            assertCondition("INACTIVE".equals(devAfter.getLicenseStatus()), "Device license_status remains INACTIVE");

            DeviceLicenseResponse licAfter = licenseService.getActiveLicenseForDevice(devZero);
            assertCondition(licAfter == null, "Zero active license records created for device");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testIdempotentDuplicateActivation(
            DeviceLicenseService licenseService, WalletService walletService,
            UUID shopAId, UUID ownerAId, UUID devA1Id) {
        System.out.println("\n--- Test Suite 3: Idempotent Duplicate Activation ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            int balBefore = walletService.getBalance(shopAId).getBalance();

            // Call activate on already active licensed device
            DeviceLicenseResponse resp = licenseService.activateLicense(new LicenseActivateRequest(devA1Id, 365, 1, "Duplicate request"));

            assertCondition(resp != null && "ACTIVE".equals(resp.getStatus()), "Idempotent response returned active license");
            int balAfter = walletService.getBalance(shopAId).getBalance();
            assertCondition(balBefore == balAfter, "Wallet balance was NOT deducted again (Before: " + balBefore + ", After: " + balAfter + ")");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testLicenseDurationAndExpiration(
            DeviceLicenseService licenseService, UUID shopAId, UUID ownerAId, UUID devA2Id) {
        System.out.println("\n--- Test Suite 4: License Duration & Expiration Calculations ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            // Activate 30-day license
            DeviceLicenseResponse resp30 = licenseService.activateLicense(new LicenseActivateRequest(devA2Id, 30, 1, "30-day trial"));
            assertCondition(resp30.getDaysRemaining() >= 29 && resp30.getDaysRemaining() <= 30,
                    "30-day license calculated daysRemaining ~30 (Got: " + resp30.getDaysRemaining() + ")");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testLicenseRevocationAndStateSync(
            DeviceLicenseService licenseService, FinancedDeviceMapper deviceMapper,
            UUID shopAId, UUID ownerAId, UUID superAdminId, UUID devA2Id) {
        System.out.println("\n--- Test Suite 5: License Revocation & State Synchronization ---");

        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            DeviceLicenseResponse activeResp = licenseService.getActiveLicenseForDevice(devA2Id);
            UUID licenseId = activeResp.getLicense().getId();

            // 1. Shop Owner revokes license
            AbizDeviceLicense revokedLic = licenseService.revokeLicense(licenseId, "Customer returned device");
            assertCondition("REVOKED".equals(revokedLic.getStatus()), "License status transitioned to REVOKED");

            // 2. Verify device status synchronized to INACTIVE
            AbizFinancedDevice dev = deviceMapper.findDeviceById(devA2Id);
            assertCondition("INACTIVE".equals(dev.getLicenseStatus()), "Device license_status synchronized to INACTIVE");

            // 3. Verify getActiveLicenseForDevice now returns null
            DeviceLicenseResponse afterRevoke = licenseService.getActiveLicenseForDevice(devA2Id);
            assertCondition(afterRevoke == null, "Device has no active license after revocation");

            // 4. Idempotent second revocation
            AbizDeviceLicense secondRevoke = licenseService.revokeLicense(licenseId, "Duplicate revoke call");
            assertCondition("REVOKED".equals(secondRevoke.getStatus()), "Repeated revocation returns REVOKED idempotently");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testRbacEnforcement(
            DeviceLicenseService licenseService, WalletService walletService,
            UUID shopAId, UUID staffAId, UUID ownerAId, UUID superAdminId, UUID devA3Id) {
        System.out.println("\n--- Test Suite 6: RBAC Enforcement for Staff, Owner, Super Admin ---");

        // 1. Staff CAN activate a license
        AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopAId, "staff.a@test.com");
        DeviceLicenseResponse resp;
        try {
            resp = licenseService.activateLicense(new LicenseActivateRequest(devA3Id, 365, 1, "Staff activation"));
            assertCondition("ACTIVE".equals(resp.getStatus()), "SHOP_STAFF permitted to activate license");
        } finally {
            AbizTenantContext.clear();
        }

        // 2. Staff CANNOT revoke a license
        AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopAId, "staff.a@test.com");
        boolean staffRevokeBlocked = false;
        try {
            licenseService.revokeLicense(resp.getLicense().getId(), "Unauthorized staff attempt");
        } catch (SecurityException e) {
            staffRevokeBlocked = true;
        } finally {
            AbizTenantContext.clear();
        }
        assertCondition(staffRevokeBlocked, "SHOP_STAFF MUST be blocked from revoking licenses (403 Forbidden)");

        // 3. Staff CANNOT add wallet credits
        AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopAId, "staff.a@test.com");
        boolean staffAddBlocked = false;
        try {
            walletService.addCredits(shopAId, 10, "Unauthorized staff credits");
        } catch (SecurityException e) {
            staffAddBlocked = true;
        } finally {
            AbizTenantContext.clear();
        }
        assertCondition(staffAddBlocked, "SHOP_STAFF MUST be blocked from adding wallet credits");

        // 4. Super Admin CAN revoke license
        AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "super@test.com");
        try {
            AbizDeviceLicense adminRevoked = licenseService.revokeLicense(resp.getLicense().getId(), "Admin policy enforcement");
            assertCondition("REVOKED".equals(adminRevoked.getStatus()), "SUPER_ADMIN permitted to revoke license");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCrossTenantSecurity(
            DeviceLicenseService licenseService, UUID shopAId, UUID shopBId,
            UUID ownerAId, UUID staffAId, UUID superAdminId, UUID devB1Id) {
        System.out.println("\n--- Test Suite 7: Cross-Tenant Isolation & Security ---");

        // 1. Shop Owner A cannot activate license for Shop B device
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        boolean ownerCrossBlocked = false;
        try {
            licenseService.activateLicense(new LicenseActivateRequest(devB1Id, 365, 1, "Cross-tenant attempt"));
        } catch (SecurityException e) {
            ownerCrossBlocked = true;
        } finally {
            AbizTenantContext.clear();
        }
        assertCondition(ownerCrossBlocked, "Shop Owner A MUST be blocked from licensing Shop B device");

        // 2. Shop Staff A cannot activate license for Shop B device
        AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopAId, "staff.a@test.com");
        boolean staffCrossBlocked = false;
        try {
            licenseService.activateLicense(new LicenseActivateRequest(devB1Id, 365, 1, "Cross-tenant staff attempt"));
        } catch (SecurityException e) {
            staffCrossBlocked = true;
        } finally {
            AbizTenantContext.clear();
        }
        assertCondition(staffCrossBlocked, "Shop Staff A MUST be blocked from licensing Shop B device");

        // 3. Super Admin can query any shop's licenses
        AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "super@test.com");
        try {
            List<AbizDeviceLicense> allLics = licenseService.listLicenses(null, null, 0, 50);
            assertCondition(allLics.size() >= 2, "SUPER_ADMIN can list licenses across all shops (Found: " + allLics.size() + ")");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testConcurrent10ThreadsOneCreditRace(
            SqlSessionFactory factory, PGSimpleDataSource dataSource,
            FinancedDeviceMapper deviceMapper,
            UUID shopAId, UUID ownerAId) throws Exception {
        System.out.println("\n--- Test Suite 8: Multi-Threaded Concurrency (10 Threads Racing for 1 Credit) ---");

        // 1. Seed Shop A wallet with EXACTLY 1 credit
        try (SqlSession setupSession = factory.openSession(false)) {
            WalletMapper setupWallet = setupSession.getMapper(WalletMapper.class);
            AuditMapper setupAudit = setupSession.getMapper(AuditMapper.class);
            AbizAuditService audit = new AbizAuditService();
            Field f = AbizAuditService.class.getDeclaredField("auditMapper");
            f.setAccessible(true);
            f.set(audit, setupAudit);

            WalletRow current = setupWallet.getWallet(shopAId);
            if (current != null && current.balance() > 0) {
                setupWallet.updateBalance(shopAId, 0, current.version());
            }
            WalletRow zeroed = setupWallet.getWallet(shopAId);
            setupWallet.updateBalance(shopAId, 1, zeroed.version());
            setupSession.commit();
        }

        // Verify initial 1 credit
        try (SqlSession checkSession = factory.openSession(true)) {
            WalletMapper checkWallet = checkSession.getMapper(WalletMapper.class);
            WalletRow w = checkWallet.getWallet(shopAId);
            assertCondition(w.balance() == 1, "Shop A wallet initialized with EXACTLY 1 credit for race condition test");
        }

        // 2. Register 10 distinct devices for Shop A
        List<UUID> deviceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID devId = UUID.randomUUID();
            insertDevice(deviceMapper, devId, shopAId, nextValidImei(), "Conc Model " + i, "BrandConc");
            deviceIds.add(devId);
        }

        // 3. Launch 10 concurrent threads each attempting to activate a license
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int tIndex = i;
            final UUID targetDevId = deviceIds.get(i);
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronized release of all 10 threads
                    AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner" + tIndex + "@test.com");

                    try (SqlSession threadSession = factory.openSession(false)) {
                        DeviceLicenseMapper threadLicenseMapper = threadSession.getMapper(DeviceLicenseMapper.class);
                        WalletMapper threadWalletMapper = threadSession.getMapper(WalletMapper.class);
                        AuditMapper threadAuditMapper = threadSession.getMapper(AuditMapper.class);

                        AbizAuditService threadAudit = new AbizAuditService();
                        Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                        f.setAccessible(true);
                        f.set(threadAudit, threadAuditMapper);

                        DeviceLicenseService threadLicService = new DeviceLicenseService(threadLicenseMapper, threadWalletMapper, threadAudit);

                        threadLicService.activateLicense(new LicenseActivateRequest(targetDevId, 365, 1, "Concurrent race " + tIndex));
                        threadSession.commit();
                        successCount.incrementAndGet();
                    }
                } catch (InsufficientCreditsException | IllegalStateException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Thread " + tIndex + " unexpected error: " + e.getMessage());
                } finally {
                    AbizTenantContext.clear();
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // FIRE ALL THREADS SIMULTANEOUSLY

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        System.out.println(String.format("  [CONCURRENCY METRICS] 10 concurrent threads: %d succeeded (1 credit consumed), %d cleanly rejected (insufficient credits)",
                successCount.get(), rejectedCount.get()));

        assertCondition(successCount.get() == 1, "EXACTLY 1 thread succeeded in consuming the single credit (Found: " + successCount.get() + ")");
        assertCondition(rejectedCount.get() == 9, "EXACTLY 9 threads were safely rejected due to insufficient credits (Found: " + rejectedCount.get() + ")");

        // 4. Direct independent PostgreSQL verification
        try (Connection conn = dataSource.getConnection();
             PreparedStatement psWallet = conn.prepareStatement("SELECT balance FROM abiz_wallets WHERE shop_id = ?");
             PreparedStatement psTxs = conn.prepareStatement("SELECT COUNT(*) FROM abiz_wallet_transactions WHERE shop_id = ? AND type = 'DEVICE_ACTIVATION_CONFIRM' AND reason LIKE 'Device license activation for device %'");
             PreparedStatement psLics = conn.prepareStatement("SELECT COUNT(*) FROM abiz_device_licenses WHERE shop_id = ? AND status = 'ACTIVE' AND financed_device_id IN (" +
                     String.join(",", Collections.nCopies(10, "?::uuid")) + ")")
        ) {
            psWallet.setObject(1, shopAId);
            try (ResultSet rs = psWallet.executeQuery()) {
                rs.next();
                int pgBal = rs.getInt(1);
                assertCondition(pgBal == 0, "PostgreSQL wallet balance is EXACTLY 0 (No negative overdraft! Got: " + pgBal + ")");
            }

            for (int i = 0; i < 10; i++) {
                psLics.setObject(i + 2, deviceIds.get(i));
            }
            psLics.setObject(1, shopAId);
            try (ResultSet rs = psLics.executeQuery()) {
                rs.next();
                int pgActiveLics = rs.getInt(1);
                assertCondition(pgActiveLics == 1, "PostgreSQL contains EXACTLY 1 active license among the 10 race devices (Found: " + pgActiveLics + ")");
            }
        }
    }

    private static void testAuditTrailLogging(PGSimpleDataSource dataSource) {
        System.out.println("\n--- Test Suite 9: Audit Trail Generation for Licensing Events ---");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT action, result, COUNT(*) AS cnt FROM abiz_audit_logs " +
                             "WHERE action IN ('LICENSE_ACTIVATED', 'LICENSE_REVOKED', 'LICENSE_REJECTED', 'CROSS_TENANT_LICENSE_ACCESS_BLOCKED') " +
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
                assertCondition(totalLogs >= 4, "License operational events recorded in abiz_audit_logs (Total: " + totalLogs + ")");
            }
        } catch (Exception e) {
            testsFailed++;
            System.err.println("Audit trail verification failed: " + e.getMessage());
        }
    }

    private static void insertDevice(FinancedDeviceMapper mapper, UUID id, UUID shopId, String imei, String model, String brand) {
        AbizFinancedDevice dev = new AbizFinancedDevice();
        dev.setId(id);
        dev.setShopId(shopId);
        dev.setImei1(imei);
        dev.setModel(model);
        dev.setManufacturer(brand);
        dev.setEnrollmentStatus("NOT_ENROLLED");
        dev.setConnectionStatus("OFFLINE");
        dev.setLockStatus("UNLOCKED");
        dev.setLicenseStatus("INACTIVE");
        dev.setPolicyState("ACTIVE");
        mapper.insertDevice(dev);
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
