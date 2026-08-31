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
import com.abiz.locker.payment.PaymentService;
import com.abiz.locker.policy.DevicePolicyService;
import com.abiz.locker.shop.ShopService;
import com.abiz.locker.wallet.WalletMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.postgresql.ds.PGSimpleDataSource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive verification test suite for ABIZ Locker Phase 14:
 * Policy State Machine & Automated Lock Policy.
 */
public class Phase14PolicyVerificationTest {

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
        System.out.println("ABIZ LOCKER — PHASE 14 POLICY STATE MACHINE VERIFICATION TEST");
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
        configuration.addMapper(EnrollmentSessionMapper.class);
        configuration.addMapper(DevicePolicyMapper.class);
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
            PaymentMapper paymentMapper = session.getMapper(PaymentMapper.class);
            DeviceLicenseMapper licenseMapper = session.getMapper(DeviceLicenseMapper.class);
            EnrollmentSessionMapper enrollmentMapper = session.getMapper(EnrollmentSessionMapper.class);
            DevicePolicyMapper policyMapper = session.getMapper(DevicePolicyMapper.class);
            AuditMapper auditMapper = session.getMapper(AuditMapper.class);

            AbizAuditService auditService = new AbizAuditService();
            Field f = AbizAuditService.class.getDeclaredField("auditMapper");
            f.setAccessible(true);
            f.set(auditService, auditMapper);

            ShopService shopService = new ShopService(shopMapper, auditService);
            CustomerService customerService = new CustomerService(customerMapper, shopMapper, auditService);
            FinancedDeviceService deviceService = new FinancedDeviceService(deviceMapper, customerMapper, auditService);
            ContractService contractService = new ContractService(contractMapper, customerMapper, deviceMapper, auditService);
            InstallmentService installmentService = new InstallmentService(installmentMapper, contractMapper, auditService);
            PaymentService paymentService = new PaymentService(paymentMapper, installmentMapper, auditService);
            DeviceLicenseService licenseService = new DeviceLicenseService(licenseMapper, walletMapper, auditService);
            EnrollmentService enrollmentService = new EnrollmentService(enrollmentMapper, contractMapper, customerMapper, licenseMapper, auditService);
            DevicePolicyService policyService = new DevicePolicyService(policyMapper, contractMapper, installmentMapper, auditService);

            // 1. Seed Super Admin Context
            UUID superAdminId = UUID.randomUUID();
            AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "super14@abizlocker.com");

            // 2. Seed Shops
            AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                    "Policy Shop A " + UUID.randomUUID().toString().substring(0, 5),
                    "Owner A", "+923001112233", "polA_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Lahore", "Lahore", "ACTIVE"
            ));
            AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                    "Policy Shop B " + UUID.randomUUID().toString().substring(0, 5),
                    "Owner B", "+923004445566", "polB_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Karachi", "Karachi", "ACTIVE"
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
            AbizCustomer custA = customerService.createCustomer(new CustomerCreateRequest("Customer 14A", "+923001119999", "35201-1119999-1", "Lahore"));

            AbizTenantContext.set(ownerBId, AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
            AbizCustomer custB = customerService.createCustomer(new CustomerCreateRequest("Customer 14B", "+923002229999", "35201-2229999-1", "Karachi"));

            AbizTenantContext.clear();

            // Run All Phase 14 Test Suites
            testInitialEnrolledPolicyState(enrollmentService, deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testFutureInstallmentPaidCurrentState(enrollmentService, deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testWarningStateTransition(enrollmentService, deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testGracePeriodTransition(enrollmentService, deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testOverdueTransition(enrollmentService, deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testLockEligibleTransition(enrollmentService, deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testPaymentRecoveryTransition(enrollmentService, deviceService, contractService, installmentService, paymentService, policyService, shopAId, ownerAId, custA.getId());
            testCompletedContractHandling(enrollmentService, deviceService, contractService, installmentService, paymentService, policyService, shopAId, ownerAId, custA.getId());
            testCancelledContractHandling(enrollmentService, deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testUnenrolledDeviceHandling(deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testTenantIsolationAndRbac(deviceService, contractService, installmentService, policyService, shopAId, shopBId, ownerAId, staffAId, superAdminId, custB.getId());
            testIdempotencyAndDuplicateTransitionPrevention(enrollmentService, deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testBatchShopEvaluation(policyService, shopAId, ownerAId);
            testConcurrentPolicyEvaluationRace(factory, dataSource, enrollmentService, deviceService, contractService, installmentService, policyService, shopAId, ownerAId, custA.getId());
            testPaymentVsPolicyEvaluationRace(factory, dataSource, enrollmentService, deviceService, contractService, installmentService, paymentService, policyService, shopAId, ownerAId, custA.getId());
            testAuditTrailLogging(dataSource);

            System.out.println("\n===============================================================");
            System.out.println(String.format("PHASE 14 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
            System.out.println("===============================================================");

            if (testsFailed > 0) {
                System.err.println("FATAL: " + testsFailed + " tests failed during Phase 14 verification.");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("FATAL PHASE 14 TEST SUITE FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testInitialEnrolledPolicyState(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 1: Initial Enrolled Policy State ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A14", "Samsung"));
            Date firstDue = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("30000.00"), firstDue);

            // Enroll device
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            PolicyEvaluationResult result = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("ACTIVE".equals(result.getNewPolicyState()), "Newly enrolled device gets ACTIVE policy state (Got: " + result.getNewPolicyState() + ")");
            assertCondition("UNLOCKED".equals(result.getNewLockStatus()), "Newly enrolled device lock_status is UNLOCKED");
            assertCondition(!result.isLockEligible(), "Newly enrolled device is not lock-eligible");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testFutureInstallmentPaidCurrentState(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 2: Future Installments / Paid Current State ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A34", "Samsung"));
            Date firstDue = Date.from(Instant.now().plus(25, ChronoUnit.DAYS));
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("40000.00"), firstDue);
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            PolicyEvaluationResult result = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("ACTIVE".equals(result.getNewPolicyState()), "Device with next due date 25 days out is ACTIVE");
            assertCondition("UNLOCKED".equals(result.getNewLockStatus()), "Lock status is UNLOCKED");
            assertCondition(result.getDaysUntilDue() >= 20, "Days until due is accurately calculated (" + result.getDaysUntilDue() + " days)");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testWarningStateTransition(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 3: Warning State Transition (Due in <= 3 Days) ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A54", "Samsung"));
            Date firstDue = Date.from(Instant.now().plus(2, ChronoUnit.DAYS)); // Due in 2 days
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("50000.00"), firstDue);
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            PolicyEvaluationResult result = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("WARNING".equals(result.getNewPolicyState()), "Device due in 2 days transitions to WARNING (Got: " + result.getNewPolicyState() + ")");
            assertCondition("UNLOCKED".equals(result.getNewLockStatus()), "Lock status is UNLOCKED in WARNING state");
            assertCondition(!result.isLockEligible(), "Device in WARNING state is not lock-eligible");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testGracePeriodTransition(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 4: Grace Period Transition (Due Date Passed <= Grace Period) ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy S23", "Samsung"));
            Date firstDue = Date.from(Instant.now().minus(2, ChronoUnit.DAYS)); // Passed 2 days ago (Grace period is 3)
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("60000.00"), firstDue);
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            PolicyEvaluationResult result = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("GRACE_PERIOD".equals(result.getNewPolicyState()), "Device 2 days past due is in GRACE_PERIOD (Got: " + result.getNewPolicyState() + ")");
            assertCondition("WARNING".equals(result.getNewLockStatus()), "Lock status is WARNING during grace period");
            assertCondition(!result.isLockEligible(), "Device in GRACE_PERIOD is not yet locked");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testOverdueTransition(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 5: Overdue State Transition (Grace Period Expired) ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy S24", "Samsung"));
            Date firstDue = Date.from(Instant.now().minus(4, ChronoUnit.DAYS)); // Passed 4 days ago (Grace period 3 expired)
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("70000.00"), firstDue);
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            PolicyEvaluationResult result = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("OVERDUE".equals(result.getNewPolicyState()), "Device 4 days past due transitions to OVERDUE (Got: " + result.getNewPolicyState() + ")");
            assertCondition("WARNING".equals(result.getNewLockStatus()), "Lock status is WARNING in OVERDUE state");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testLockEligibleTransition(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 6: Lock-Eligible Transition (Lock Threshold Exceeded) ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy Z Fold", "Samsung"));
            Date firstDue = Date.from(Instant.now().minus(6, ChronoUnit.DAYS)); // 6 days overdue (Grace 3 + 1 threshold exceeded)
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("100000.00"), firstDue);
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            PolicyEvaluationResult result = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("LOCKED".equals(result.getNewPolicyState()), "Device 6 days overdue transitions to LOCKED (Got: " + result.getNewPolicyState() + ")");
            assertCondition("LOCKED".equals(result.getNewLockStatus()), "Lock status is LOCKED");
            assertCondition(result.isLockEligible(), "Device is marked lock-eligible");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testPaymentRecoveryTransition(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 7: Payment Recovery Transition (LOCKED -> ACTIVE) ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Redmi Note 12", "Xiaomi"));
            Date firstDue = Date.from(Instant.now().minus(7, ChronoUnit.DAYS)); // Overdue and locked
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("20000.00"), firstDue);
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            // Step 1: Confirm initial LOCKED state
            PolicyEvaluationResult lockResult = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("LOCKED".equals(lockResult.getNewPolicyState()), "Device is initially LOCKED");

            // Step 2: Customer pays the first installment amount
            PaymentRecordRequest payReq = new PaymentRecordRequest(contract.getId(), null, new BigDecimal("10000.00"), "CASH", "REF-001", "Overdue payoff");
            paymentService.recordPayment(payReq);

            // Step 3: Re-evaluate policy
            PolicyEvaluationResult recoverResult = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition(recoverResult.isRecovery(), "Evaluation identified a policy recovery event");
            assertCondition("ACTIVE".equals(recoverResult.getNewPolicyState()) || "WARNING".equals(recoverResult.getNewPolicyState()),
                    "Device recovered to healthy policy state (Got: " + recoverResult.getNewPolicyState() + ")");
            assertCondition("UNLOCKED".equals(recoverResult.getNewLockStatus()), "Lock status successfully reset to UNLOCKED");
            assertCondition(!recoverResult.isLockEligible(), "Device is no longer lock-eligible");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCompletedContractHandling(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 8: Completed Contract Handling (RELEASED State) ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Oppo Reno 10", "Oppo"));
            Date firstDue = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("10000.00"), firstDue);
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            // Pay in full
            paymentService.recordPayment(new PaymentRecordRequest(contract.getId(), null, new BigDecimal("10000.00"), "CASH", "PAY-FULL", "Full payoff"));

            PolicyEvaluationResult result = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("RELEASED".equals(result.getNewPolicyState()), "Fully paid contract device transitions to RELEASED (Got: " + result.getNewPolicyState() + ")");
            assertCondition("UNLOCKED".equals(result.getNewLockStatus()), "Lock status is UNLOCKED for RELEASED device");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCancelledContractHandling(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 9: Cancelled Contract Handling ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Infinix Note 30", "Infinix"));
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("15000.00"), new Date());
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            contractService.cancelContract(contract.getId(), "Merchant cancellation");

            PolicyEvaluationResult result = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("RELEASED".equals(result.getNewPolicyState()), "Cancelled contract device transitions to RELEASED (Got: " + result.getNewPolicyState() + ")");
            assertCondition("UNLOCKED".equals(result.getNewLockStatus()), "Lock status is UNLOCKED");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testUnenrolledDeviceHandling(
            FinancedDeviceService deviceService, ContractService contractService,
            InstallmentService installmentService, DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 10: Unenrolled Device Policy Handling ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Tecno Spark 10", "Tecno"));
            Date overdueDate = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("12000.00"), overdueDate);

            // Device is NOT enrolled
            PolicyEvaluationResult result = policyService.evaluateDevicePolicy(dev.getId());
            assertCondition("ACTIVE".equals(result.getNewPolicyState()), "Unenrolled device remains in ACTIVE policy state without lock");
            assertCondition("UNLOCKED".equals(result.getNewLockStatus()), "Unenrolled device lock status is UNLOCKED");
            assertCondition(!result.isLockEligible(), "Unenrolled device is not eligible for lock enforcement");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testTenantIsolationAndRbac(
            FinancedDeviceService deviceService, ContractService contractService,
            InstallmentService installmentService, DevicePolicyService policyService,
            UUID shopAId, UUID shopBId, UUID ownerAId, UUID staffAId, UUID superAdminId, UUID custBId) {
        System.out.println("\n--- Test Suite 11: Tenant Isolation & RBAC ---");

        // Seed device for Shop B
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
        AbizFinancedDevice devB = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Vivo V29", "Vivo"));
        createContractWithDue(contractService, installmentService, custBId, devB.getId(), new BigDecimal("45000.00"), new Date());
        AbizTenantContext.clear();

        // 1. Shop Owner A evaluating Shop B device
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        boolean ownerCrossBlocked = false;
        try {
            policyService.evaluateDevicePolicy(devB.getId());
        } catch (SecurityException e) {
            ownerCrossBlocked = true;
        } finally {
            AbizTenantContext.clear();
        }
        assertCondition(ownerCrossBlocked, "Shop Owner A MUST be blocked from evaluating Shop B device (403 Forbidden)");

        // 2. Shop Staff A evaluating Shop B device
        AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopAId, "staff.a@test.com");
        boolean staffCrossBlocked = false;
        try {
            policyService.evaluateDevicePolicy(devB.getId());
        } catch (SecurityException e) {
            staffCrossBlocked = true;
        } finally {
            AbizTenantContext.clear();
        }
        assertCondition(staffCrossBlocked, "Shop Staff A MUST be blocked from evaluating Shop B device (403 Forbidden)");

        // 3. Super Admin global evaluation
        AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "super@test.com");
        try {
            PolicyEvaluationResult adminResult = policyService.evaluateDevicePolicy(devB.getId());
            assertCondition(adminResult != null, "SUPER_ADMIN can evaluate policy for any shop device");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testIdempotencyAndDuplicateTransitionPrevention(
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 12: Idempotency & Duplicate Transition Prevention ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Realme C55", "Realme"));
            Date firstDue = Date.from(Instant.now().plus(20, ChronoUnit.DAYS));
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, dev.getId(), new BigDecimal("25000.00"), firstDue);
            enrollDeviceFully(enrollmentService, dev.getId(), contract.getId(), custAId);

            // First evaluation
            PolicyEvaluationResult r1 = policyService.evaluateDevicePolicy(dev.getId());
            DevicePolicyStateResponse resp1 = policyService.getDevicePolicyState(dev.getId());
            int historyCount1 = resp1.getHistory().size();

            // Second evaluation without data change
            PolicyEvaluationResult r2 = policyService.evaluateDevicePolicy(dev.getId());
            DevicePolicyStateResponse resp2 = policyService.getDevicePolicyState(dev.getId());
            int historyCount2 = resp2.getHistory().size();

            assertCondition(!r2.isTransitionApplied(), "Repeated evaluation with unchanged data does NOT apply a transition");
            assertCondition(r1.getNewPolicyState().equals(r2.getNewPolicyState()), "Resulting policy state is identical across repeated calls");
            assertCondition(historyCount1 == historyCount2, "No duplicate history records created on idempotent evaluation");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testBatchShopEvaluation(
            DevicePolicyService policyService, UUID shopAId, UUID ownerAId) {
        System.out.println("\n--- Test Suite 13: Batch Shop Policy Evaluation ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            List<PolicyEvaluationResult> batchResults = policyService.evaluateShopDevices(shopAId);
            assertCondition(batchResults != null && batchResults.size() >= 5, "Batch evaluation processed all shop devices (Count: " + batchResults.size() + ")");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testConcurrentPolicyEvaluationRace(
            SqlSessionFactory factory, PGSimpleDataSource dataSource,
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 14: Multi-Threaded Concurrency (10 Threads Evaluating 1 Device) ---");

        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        final UUID targetDevId;
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Race Device", "Samsung"));
            targetDevId = dev.getId();
            Date overdueDate = Date.from(Instant.now().minus(8, ChronoUnit.DAYS)); // Overdue
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, targetDevId, new BigDecimal("35000.00"), overdueDate);
            enrollDeviceFully(enrollmentService, targetDevId, contract.getId(), custAId);
        } finally {
            AbizTenantContext.clear();
        }

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    try (SqlSession threadSession = factory.openSession(true)) {
                        DevicePolicyMapper threadPolicyMapper = threadSession.getMapper(DevicePolicyMapper.class);
                        ContractMapper threadContractMapper = threadSession.getMapper(ContractMapper.class);
                        InstallmentMapper threadInstallmentMapper = threadSession.getMapper(InstallmentMapper.class);
                        AuditMapper threadAuditMapper = threadSession.getMapper(AuditMapper.class);

                        AbizAuditService threadAudit = new AbizAuditService();
                        Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                        f.setAccessible(true);
                        f.set(threadAudit, threadAuditMapper);

                        DevicePolicyService threadPolicyService = new DevicePolicyService(
                                threadPolicyMapper, threadContractMapper, threadInstallmentMapper, threadAudit
                        );

                        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                        PolicyEvaluationResult res = threadPolicyService.evaluateDevicePolicy(targetDevId);
                        if ("LOCKED".equals(res.getNewPolicyState())) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Concurrent thread error: " + e.getMessage());
                } finally {
                    AbizTenantContext.clear();
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // FIRE ALL 10 THREADS

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertCondition(successCount.get() == 10, "All 10 concurrent evaluations completed successfully with deterministic LOCKED state");

        // Verify direct PostgreSQL state
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT policy_state, lock_status FROM abiz_financed_devices WHERE id = ?");
             PreparedStatement psHist = conn.prepareStatement("SELECT COUNT(*) FROM abiz_device_policy_states WHERE financed_device_id = ?")
        ) {
            ps.setObject(1, targetDevId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertCondition("LOCKED".equals(rs.getString("policy_state")), "PostgreSQL device policy_state is LOCKED");
                assertCondition("LOCKED".equals(rs.getString("lock_status")), "PostgreSQL device lock_status is LOCKED");
            }

            psHist.setObject(1, targetDevId);
            try (ResultSet rs = psHist.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                assertCondition(count >= 1, "AT LEAST 1 history transition record created by concurrent policy evaluation (Found: " + count + ")");
            }
        }
    }

    private static void testPaymentVsPolicyEvaluationRace(
            SqlSessionFactory factory, PGSimpleDataSource dataSource,
            EnrollmentService enrollmentService, FinancedDeviceService deviceService,
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, DevicePolicyService policyService,
            UUID shopAId, UUID ownerAId, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 15: Payment vs Policy Evaluation Race ---");

        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        final UUID targetDevId;
        final UUID targetContractId;
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Payment Race Device", "Samsung"));
            targetDevId = dev.getId();
            Date overdueDate = Date.from(Instant.now().minus(8, ChronoUnit.DAYS)); // Overdue
            AbizContract contract = createContractWithDue(contractService, installmentService, custAId, targetDevId, new BigDecimal("20000.00"), overdueDate);
            targetContractId = contract.getId();
            enrollDeviceFully(enrollmentService, targetDevId, targetContractId, custAId);
        } finally {
            AbizTenantContext.clear();
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        // Thread 1: Record payment
        Future<?> f1 = executor.submit(() -> {
            try {
                startLatch.await();
                try (SqlSession threadSession = factory.openSession(true)) {
                    PaymentMapper threadPaymentMapper = threadSession.getMapper(PaymentMapper.class);
                    InstallmentMapper threadInstallmentMapper = threadSession.getMapper(InstallmentMapper.class);
                    AuditMapper threadAuditMapper = threadSession.getMapper(AuditMapper.class);

                    AbizAuditService threadAudit = new AbizAuditService();
                    Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                    f.setAccessible(true);
                    f.set(threadAudit, threadAuditMapper);

                    PaymentService threadPaymentService = new PaymentService(threadPaymentMapper, threadInstallmentMapper, threadAudit);

                    AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                    threadPaymentService.recordPayment(new PaymentRecordRequest(targetContractId, null, new BigDecimal("10000.00"), "CASH", "RACE-PAY", "Race payment"));
                }
            } catch (Exception e) {
                System.err.println("Payment thread error: " + e.getMessage());
            } finally {
                AbizTenantContext.clear();
            }
        });

        // Thread 2: Evaluate policy
        Future<?> f2 = executor.submit(() -> {
            try {
                startLatch.await();
                try (SqlSession threadSession = factory.openSession(true)) {
                    DevicePolicyMapper threadPolicyMapper = threadSession.getMapper(DevicePolicyMapper.class);
                    ContractMapper threadContractMapper = threadSession.getMapper(ContractMapper.class);
                    InstallmentMapper threadInstallmentMapper = threadSession.getMapper(InstallmentMapper.class);
                    AuditMapper threadAuditMapper = threadSession.getMapper(AuditMapper.class);

                    AbizAuditService threadAudit = new AbizAuditService();
                    Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                    f.setAccessible(true);
                    f.set(threadAudit, threadAuditMapper);

                    DevicePolicyService threadPolicyService = new DevicePolicyService(
                            threadPolicyMapper, threadContractMapper, threadInstallmentMapper, threadAudit
                    );

                    AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                    threadPolicyService.evaluateDevicePolicy(targetDevId);
                }
            } catch (Exception e) {
                System.err.println("Policy thread error: " + e.getMessage());
            } finally {
                AbizTenantContext.clear();
            }
        });

        startLatch.countDown();
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Final policy evaluation to ensure convergence
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            PolicyEvaluationResult finalRes = policyService.evaluateDevicePolicy(targetDevId);
            assertCondition(finalRes != null, "Final converged policy evaluation completed");
            assertCondition("UNLOCKED".equals(finalRes.getNewLockStatus()) || "WARNING".equals(finalRes.getNewLockStatus()),
                    "Final converged lock status is healthy (Got: " + finalRes.getNewLockStatus() + ")");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testAuditTrailLogging(PGSimpleDataSource dataSource) {
        System.out.println("\n--- Test Suite 16: Audit Trail Generation for Policy Events ---");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT action, result, COUNT(*) AS cnt FROM abiz_audit_logs " +
                             "WHERE action IN ('POLICY_STATE_CHANGED', 'DEVICE_POLICY_OVERDUE', 'DEVICE_POLICY_GRACE_PERIOD', 'DEVICE_POLICY_LOCKED', 'DEVICE_POLICY_RECOVERED', 'DEVICE_POLICY_RELEASED', 'CROSS_TENANT_POLICY_ACCESS_BLOCKED') " +
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
                assertCondition(totalLogs >= 6, "Policy operational events recorded in abiz_audit_logs (Total: " + totalLogs + ")");
            }
        } catch (Exception e) {
            testsFailed++;
            System.err.println("Audit trail verification failed: " + e.getMessage());
        }
    }

    private static AbizContract createContractWithDue(
            ContractService contractService, InstallmentService installmentService,
            UUID custId, UUID devId, BigDecimal price, Date firstDue) {
        ContractCreateRequest req = new ContractCreateRequest();
        req.setCustomerId(custId);
        req.setFinancedDeviceId(devId);
        req.setDevicePrice(price);
        req.setDownPayment(BigDecimal.ZERO);
        req.setFinancedAmount(price);
        req.setInstallmentAmount(price.divide(new BigDecimal("2")));
        req.setNumberOfInstallments(2);
        req.setFirstDueDate(firstDue);
        req.setFrequency("MONTHLY");

        AbizContract contract = contractService.createContract(req);
        contractService.activateContract(contract.getId());
        installmentService.generateSchedule(contract.getId());
        return contract;
    }

    private static void enrollDeviceFully(EnrollmentService enrollmentService, UUID devId, UUID contractId, UUID custId) {
        EnrollmentSessionResponse sessionResp = enrollmentService.createSession(new EnrollmentSessionRequest(devId, contractId, custId));
        DeviceEnrollmentCompletionRequest compReq = new DeviceEnrollmentCompletionRequest();
        compReq.setToken(sessionResp.getQrToken());
        compReq.setMdmDeviceNumber("MDM-DEV-" + UUID.randomUUID().toString().substring(0, 8));
        compReq.setAndroidVersion("14");
        compReq.setAgentVersion("2.1.0");
        enrollmentService.completeEnrollment(compReq);
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
