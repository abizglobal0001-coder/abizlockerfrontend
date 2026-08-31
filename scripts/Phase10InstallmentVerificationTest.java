package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.contract.ContractService;
import com.abiz.locker.customer.CustomerService;
import com.abiz.locker.device.FinancedDeviceService;
import com.abiz.locker.domain.*;
import com.abiz.locker.installment.InstallmentService;
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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Automated Verification Test Suite for Phase 10 — Installment Schedule & Management.
 * Exercises real PostgreSQL 18.0 database execution across all required scenarios.
 */
public class Phase10InstallmentVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static long imeiSequence = 86000000000000L + (System.currentTimeMillis() % 10000000000L);

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 10 INSTALLMENT SCHEDULE TEST SUITE");
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
            config.addMapper(ContractMapper.class);
            config.addMapper(InstallmentMapper.class);

            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = factory.openSession(true)) {
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);
                CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);
                FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);
                ContractMapper contractMapper = session.getMapper(ContractMapper.class);
                InstallmentMapper installmentMapper = session.getMapper(InstallmentMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                Field auditMapperField = AbizAuditService.class.getDeclaredField("auditMapper");
                auditMapperField.setAccessible(true);
                auditMapperField.set(auditService, auditMapper);

                ShopService shopService = new ShopService(shopMapper, auditService);
                CustomerService customerService = new CustomerService(customerMapper, shopMapper, auditService);
                FinancedDeviceService deviceService = new FinancedDeviceService(deviceMapper, customerMapper, auditService);
                ContractService contractService = new ContractService(contractMapper, customerMapper, deviceMapper, auditService);
                InstallmentService installmentService = new InstallmentService(installmentMapper, contractMapper, auditService);

                UUID superAdminId = UUID.randomUUID();
                AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");

                // Seed Test Shops
                AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                        "Inst Shop A " + UUID.randomUUID().toString().substring(0, 5),
                        "Owner A", "+923008881111", "instA@test.com", "Addr", "Lahore", "ACTIVE"
                ));
                AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                        "Inst Shop B " + UUID.randomUUID().toString().substring(0, 5),
                        "Owner B", "+923008882222", "instB@test.com", "Addr", "Karachi", "ACTIVE"
                ));
                UUID shopAId = shopA.getId();
                UUID shopBId = shopB.getId();

                // Seed Customers
                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                AbizCustomer custA = customerService.createCustomer(new CustomerCreateRequest(
                        "Cust A", "+923004441111", "35201-4441111-1", "Lahore"
                ));
                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
                AbizCustomer custB = customerService.createCustomer(new CustomerCreateRequest(
                        "Cust B", "+923004442222", "35201-4442222-1", "Karachi"
                ));

                // Seed Devices
                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                AbizFinancedDevice devA1 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Redmi 12", "Xiaomi"));
                AbizFinancedDevice devA2 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Redmi Note 12", "Xiaomi"));

                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
                AbizFinancedDevice devB1 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Infinix Hot 30", "Infinix"));

                AbizTenantContext.clear();

                UUID ownerAId = UUID.randomUUID();
                UUID staffAId = UUID.randomUUID();

                // Run All Phase 10 Test Suites
                testFinancialReconciliation(contractService, installmentService, shopAId, ownerAId, custA.getId(), devA1.getId());
                testExactInstallmentNumberingAndGaps(contractService, installmentService, shopAId, ownerAId, custA.getId());
                testMonthlyDateProgression(contractService, installmentService, shopAId, ownerAId, custA.getId());
                testLeapYearMonthlyProgression(contractService, installmentService, shopAId, ownerAId, custA.getId());
                testWeeklyDateProgression(contractService, installmentService, shopAId, ownerAId, custA.getId());
                testCustomDateProgression(contractService, installmentService, shopAId, ownerAId, custA.getId());
                testStatusEvaluationLogic(installmentService);
                testActiveContractIdempotency(contractService, installmentService, shopAId, ownerAId, custA.getId(), devA2.getId());
                testDraftRegeneration(contractService, installmentService, shopAId, ownerAId, custA.getId());
                testMultiThreadedConcurrentGeneration(factory, contractService, shopAId, ownerAId, custA.getId());
                testTenantIsolationAndRbac(contractService, installmentService, shopAId, shopBId, ownerAId, staffAId, superAdminId, custA.getId(), custB.getId(), devB1.getId());
                testAuditTrailVerification(session);

                System.out.println("\n===============================================================");
                System.out.println(String.format("PHASE 10 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
                System.out.println("===============================================================");

                if (testsFailed > 0) {
                    System.exit(1);
                }
            }

        } catch (Exception e) {
            System.err.println("FATAL PHASE 10 TEST SUITE FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testFinancialReconciliation(
            ContractService contractService, InstallmentService installmentService,
            UUID shopA, UUID ownerA, UUID custAId, UUID devAId) {
        System.out.println("\n--- Test Suite 1: Financial Reconciliation & Cent Rounding Absorption ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // Contract financedAmount = 1000.00, installments = 3 (1000.00 / 3 = 333.333...)
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(devAId);
            req.setDevicePrice(new BigDecimal("1200.00"));
            req.setDownPayment(new BigDecimal("200.00"));
            req.setFinancedAmount(new BigDecimal("1000.00"));
            req.setInstallmentAmount(new BigDecimal("333.33"));
            req.setNumberOfInstallments(3);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            contractService.activateContract(contract.getId());

            InstallmentScheduleResponse response = installmentService.generateSchedule(contract.getId());
            assertCondition(response != null, "Schedule generated successfully");
            assertCondition(response.getTotalInstallments() == 3, "Schedule contains exactly 3 installments");

            List<AbizInstallment> list = response.getInstallments();
            assertCondition(list.size() == 3, "Installment list contains 3 rows");

            // Installment 1: 333.33
            assertCondition(new BigDecimal("333.33").compareTo(list.get(0).getAmount()) == 0,
                    "Installment 1 amount is 333.33 (Got: " + list.get(0).getAmount() + ")");
            // Installment 2: 333.33
            assertCondition(new BigDecimal("333.33").compareTo(list.get(1).getAmount()) == 0,
                    "Installment 2 amount is 333.33 (Got: " + list.get(1).getAmount() + ")");
            // Installment 3 (Final remainder absorption): 1000.00 - 666.66 = 333.34
            assertCondition(new BigDecimal("333.34").compareTo(list.get(2).getAmount()) == 0,
                    "Installment 3 absorbed rounding remainder and is 333.34 (Got: " + list.get(2).getAmount() + ")");

            // Exact reconciliation: SUM == 1000.00
            BigDecimal sum = list.get(0).getAmount().add(list.get(1).getAmount()).add(list.get(2).getAmount());
            assertCondition(new BigDecimal("1000.00").compareTo(sum) == 0,
                    "SUM(installments) matches financed amount 1000.00 to exact cent");

            // Initial balances: paid_amount = 0.00, remaining_amount = amount
            for (AbizInstallment inst : list) {
                assertCondition(BigDecimal.ZERO.compareTo(inst.getPaidAmount()) == 0,
                        "Installment " + inst.getInstallmentNum() + " initial paid_amount is 0.00");
                assertCondition(inst.getAmount().compareTo(inst.getRemainingAmount()) == 0,
                        "Installment " + inst.getInstallmentNum() + " remaining_amount equals amount");
            }

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testExactInstallmentNumberingAndGaps(
            ContractService contractService, InstallmentService installmentService,
            UUID shopA, UUID ownerA, UUID custAId) {
        System.out.println("\n--- Test Suite 2: Exact Number of Installments & Sequential Gapless Numbering ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            int n = 12;
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setDevicePrice(new BigDecimal("60000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("60000.00"));
            req.setInstallmentAmount(new BigDecimal("5000.00"));
            req.setNumberOfInstallments(n);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            InstallmentScheduleResponse resp = installmentService.generateSchedule(contract.getId());

            assertCondition(resp.getInstallments().size() == n, "Exactly " + n + " installments generated");
            for (int i = 0; i < n; i++) {
                int expectedNum = i + 1;
                int actualNum = resp.getInstallments().get(i).getInstallmentNum();
                assertCondition(expectedNum == actualNum, "Installment sequence index " + i + " is " + expectedNum);
            }

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testMonthlyDateProgression(
            ContractService contractService, InstallmentService installmentService,
            UUID shopA, UUID ownerA, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 3: Monthly Date Progression Relative to firstDueDate (Jan 31 Non-Leap) ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // firstDueDate = 2027-01-31 (2027 is non-leap year)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date jan31 = sdf.parse("2027-01-31");

            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setDevicePrice(new BigDecimal("30000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("30000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(3);
            req.setFirstDueDate(jan31);
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            InstallmentScheduleResponse resp = installmentService.generateSchedule(contract.getId());

            List<AbizInstallment> list = resp.getInstallments();
            String date1 = sdf.format(list.get(0).getDueDate());
            String date2 = sdf.format(list.get(1).getDueDate());
            String date3 = sdf.format(list.get(2).getDueDate());

            assertCondition("2027-01-31".equals(date1), "Installment 1 is 2027-01-31 (Got: " + date1 + ")");
            assertCondition("2027-02-28".equals(date2), "Installment 2 is 2027-02-28 in non-leap year (Got: " + date2 + ")");
            assertCondition("2027-03-31".equals(date3), "Installment 3 relative to Jan 31 is 2027-03-31 (NOT Mar 28! Got: " + date3 + ")");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testLeapYearMonthlyProgression(
            ContractService contractService, InstallmentService installmentService,
            UUID shopA, UUID ownerA, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 4: Leap Year Monthly Progression (Jan 31, 2028) ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // firstDueDate = 2028-01-31 (2028 IS A LEAP YEAR!)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date jan31_2028 = sdf.parse("2028-01-31");

            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setDevicePrice(new BigDecimal("30000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("30000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(3);
            req.setFirstDueDate(jan31_2028);
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            InstallmentScheduleResponse resp = installmentService.generateSchedule(contract.getId());

            List<AbizInstallment> list = resp.getInstallments();
            String date1 = sdf.format(list.get(0).getDueDate());
            String date2 = sdf.format(list.get(1).getDueDate());
            String date3 = sdf.format(list.get(2).getDueDate());

            assertCondition("2028-01-31".equals(date1), "Leap Year Installment 1 is 2028-01-31 (Got: " + date1 + ")");
            assertCondition("2028-02-29".equals(date2), "Leap Year Installment 2 is 2028-02-29 (Got: " + date2 + ")");
            assertCondition("2028-03-31".equals(date3), "Leap Year Installment 3 is 2028-03-31 (Got: " + date3 + ")");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testWeeklyDateProgression(
            ContractService contractService, InstallmentService installmentService,
            UUID shopA, UUID ownerA, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 5: Weekly Date Progression (7-Day Advances) ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startMonday = sdf.parse("2026-09-07"); // Monday

            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setDevicePrice(new BigDecimal("21000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("21000.00"));
            req.setInstallmentAmount(new BigDecimal("7000.00"));
            req.setNumberOfInstallments(3);
            req.setFirstDueDate(startMonday);
            req.setFrequency("WEEKLY");

            AbizContract contract = contractService.createContract(req);
            InstallmentScheduleResponse resp = installmentService.generateSchedule(contract.getId());

            List<AbizInstallment> list = resp.getInstallments();
            String date1 = sdf.format(list.get(0).getDueDate());
            String date2 = sdf.format(list.get(1).getDueDate());
            String date3 = sdf.format(list.get(2).getDueDate());

            assertCondition("2026-09-07".equals(date1), "Weekly Installment 1 is 2026-09-07 (Got: " + date1 + ")");
            assertCondition("2026-09-14".equals(date2), "Weekly Installment 2 is 2026-09-14 (Got: " + date2 + ")");
            assertCondition("2026-09-21".equals(date3), "Weekly Installment 3 is 2026-09-21 (Got: " + date3 + ")");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCustomDateProgression(
            ContractService contractService, InstallmentService installmentService,
            UUID shopA, UUID ownerA, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 6: Custom Date Progression (30-Day Advances) ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date start = sdf.parse("2026-01-01");

            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setDevicePrice(new BigDecimal("15000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("15000.00"));
            req.setInstallmentAmount(new BigDecimal("5000.00"));
            req.setNumberOfInstallments(3);
            req.setFirstDueDate(start);
            req.setFrequency("CUSTOM");

            AbizContract contract = contractService.createContract(req);
            InstallmentScheduleResponse resp = installmentService.generateSchedule(contract.getId());

            List<AbizInstallment> list = resp.getInstallments();
            String date1 = sdf.format(list.get(0).getDueDate());
            String date2 = sdf.format(list.get(1).getDueDate());
            String date3 = sdf.format(list.get(2).getDueDate());

            assertCondition("2026-01-01".equals(date1), "Custom Installment 1 is 2026-01-01 (Got: " + date1 + ")");
            assertCondition("2026-01-31".equals(date2), "Custom Installment 2 is +30 days: 2026-01-31 (Got: " + date2 + ")");
            assertCondition("2026-03-02".equals(date3), "Custom Installment 3 is +60 days: 2026-03-02 (Got: " + date3 + ")");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testStatusEvaluationLogic(InstallmentService installmentService) {
        System.out.println("\n--- Test Suite 7: Status Evaluation Timing Logic ---");
        LocalDate refDate = LocalDate.of(2026, 8, 30);
        int graceDays = 3;

        // 1. Future date -> UPCOMING
        LocalDate futureDate = LocalDate.of(2026, 9, 15);
        String sUpcoming = InstallmentService.evaluateStatus(futureDate, graceDays, refDate);
        assertCondition("UPCOMING".equals(sUpcoming), "Reference before due date evaluates to UPCOMING");

        // 2. Exactly on due date -> DUE
        LocalDate todayDate = LocalDate.of(2026, 8, 30);
        String sDue = InstallmentService.evaluateStatus(todayDate, graceDays, refDate);
        assertCondition("DUE".equals(sDue), "Reference equal to due date evaluates to DUE");

        // 3. 2 days past due date (within 3 day grace) -> GRACE_PERIOD
        LocalDate graceDate = LocalDate.of(2026, 8, 28);
        String sGrace = InstallmentService.evaluateStatus(graceDate, graceDays, refDate);
        assertCondition("GRACE_PERIOD".equals(sGrace), "Reference inside grace period evaluates to GRACE_PERIOD");

        // 4. Exactly on last day of grace period -> GRACE_PERIOD
        LocalDate lastGraceDate = LocalDate.of(2026, 8, 27); // 27 + 3 days = 30
        String sLastGrace = InstallmentService.evaluateStatus(lastGraceDate, graceDays, refDate);
        assertCondition("GRACE_PERIOD".equals(sLastGrace), "Reference on boundary of grace period evaluates to GRACE_PERIOD");

        // 5. 4 days past due date (past 3 day grace) -> OVERDUE
        LocalDate overdueDate = LocalDate.of(2026, 8, 26); // 26 + 3 days = 29 < 30
        String sOverdue = InstallmentService.evaluateStatus(overdueDate, graceDays, refDate);
        assertCondition("OVERDUE".equals(sOverdue), "Reference past grace period evaluates to OVERDUE");

        // 6. Zero grace period boundary condition
        LocalDate zeroGraceDate = LocalDate.of(2026, 8, 29);
        String sZeroGrace = InstallmentService.evaluateStatus(zeroGraceDate, 0, refDate);
        assertCondition("OVERDUE".equals(sZeroGrace), "With gracePeriod=0, day after due date immediately evaluates to OVERDUE");
    }

    private static void testActiveContractIdempotency(
            ContractService contractService, InstallmentService installmentService,
            UUID shopA, UUID ownerA, UUID custAId, UUID devAId) {
        System.out.println("\n--- Test Suite 8: ACTIVE Contract Schedule Idempotency ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(devAId);
            req.setDevicePrice(new BigDecimal("50000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("50000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(5);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            contractService.activateContract(contract.getId());

            // 1st Generation
            InstallmentScheduleResponse resp1 = installmentService.generateSchedule(contract.getId());
            assertCondition(resp1.getInstallments().size() == 5, "First generation generated 5 installments");

            // 2nd Generation (Idempotent call on ACTIVE contract)
            InstallmentScheduleResponse resp2 = installmentService.generateSchedule(contract.getId());
            assertCondition(resp2.getInstallments().size() == 5, "Second call returned existing 5 installments");

            // Verify exact ID, amount, and due-date identity
            for (int i = 0; i < 5; i++) {
                UUID id1 = resp1.getInstallments().get(i).getId();
                UUID id2 = resp2.getInstallments().get(i).getId();
                assertCondition(id1.equals(id2), "Installment " + (i + 1) + " retained identical ID across calls");
                assertCondition(resp1.getInstallments().get(i).getAmount().compareTo(
                        resp2.getInstallments().get(i).getAmount()) == 0,
                        "Installment " + (i + 1) + " retained identical amount");
            }

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testDraftRegeneration(
            ContractService contractService, InstallmentService installmentService,
            UUID shopA, UUID ownerA, UUID custAId) {
        System.out.println("\n--- Test Suite 9: DRAFT Contract Schedule Regeneration ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setDevicePrice(new BigDecimal("40000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("40000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(4);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            InstallmentScheduleResponse resp1 = installmentService.generateSchedule(contract.getId());
            assertCondition(resp1.getInstallments().size() == 4, "Initial draft schedule generated 4 installments");

            // Regenerate draft schedule
            InstallmentScheduleResponse resp2 = installmentService.generateSchedule(contract.getId());
            assertCondition(resp2.getInstallments().size() == 4, "Regenerated draft schedule returned 4 installments");

            // Newly generated IDs indicate old unpaid draft rows were cleanly recreated
            UUID oldId = resp1.getInstallments().get(0).getId();
            UUID newId = resp2.getInstallments().get(0).getId();
            assertCondition(!oldId.equals(newId), "DRAFT contract schedule rows successfully regenerated with fresh IDs");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testMultiThreadedConcurrentGeneration(
            SqlSessionFactory factory, ContractService contractService,
            UUID shopA, UUID ownerA, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 10: Multi-Threaded Concurrent Generation (10 Threads) ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        final UUID targetContractId;
        try {
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setDevicePrice(new BigDecimal("70000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("70000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(7);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            targetContractId = contract.getId();
        } finally {
            AbizTenantContext.clear();
        }

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int tIndex = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronized release of all 10 threads

                    AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner" + tIndex + "@test.com");

                    try (SqlSession threadSession = factory.openSession(true)) {
                        InstallmentMapper threadInstMapper = threadSession.getMapper(InstallmentMapper.class);
                        ContractMapper threadContMapper = threadSession.getMapper(ContractMapper.class);
                        AuditMapper threadAuditMapper = threadSession.getMapper(AuditMapper.class);

                        AbizAuditService threadAudit = new AbizAuditService();
                        Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                        f.setAccessible(true);
                        f.set(threadAudit, threadAuditMapper);

                        InstallmentService threadService = new InstallmentService(threadInstMapper, threadContMapper, threadAudit);

                        InstallmentScheduleResponse resp = threadService.generateSchedule(targetContractId);
                        if (resp != null && resp.getInstallments().size() == 7) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Thread " + tIndex + " error: " + e.getMessage());
                    errorCount.incrementAndGet();
                } finally {
                    AbizTenantContext.clear();
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        System.out.println(String.format("  [CONCURRENCY METRICS] 10 concurrent threads: %d succeeded with valid schedule, %d errors",
                successCount.get(), errorCount.get()));

        assertCondition(successCount.get() == 10, "All 10 concurrent requests received a valid schedule (Graceful Idempotency via SQLState 23505)");
        assertCondition(errorCount.get() == 0, "Zero unhandled exceptions or internal errors exposed under concurrency");

        // Directly query PostgreSQL to verify exact row count
        try (SqlSession verifySession = factory.openSession(true)) {
            Connection conn = verifySession.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(1) FROM abiz_installments WHERE contract_id = ?")) {
                ps.setObject(1, targetContractId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int dbCount = rs.getInt(1);
                    assertCondition(dbCount == 7, "PostgreSQL contains EXACTLY 7 rows for contract (No duplicates! Found: " + dbCount + ")");
                }
            }
        }
    }

    private static void testTenantIsolationAndRbac(
            ContractService contractService, InstallmentService installmentService,
            UUID shopA, UUID shopB, UUID ownerA, UUID staffA, UUID superAdminId,
            UUID custAId, UUID custBId, UUID devBId) {
        System.out.println("\n--- Test Suite 11: Strict Tenant Isolation & RBAC ---");

        // 1. Create and generate contract schedule in Shop B
        UUID ownerB = UUID.randomUUID();
        UUID contractBId;
        UUID installmentBId;
        AbizTenantContext.set(ownerB, AbizRole.SHOP_OWNER, shopB, "owner.b@test.com");
        try {
            ContractCreateRequest bReq = new ContractCreateRequest();
            bReq.setCustomerId(custBId);
            bReq.setFinancedDeviceId(devBId);
            bReq.setDevicePrice(new BigDecimal("45000.00"));
            bReq.setDownPayment(BigDecimal.ZERO);
            bReq.setFinancedAmount(new BigDecimal("45000.00"));
            bReq.setInstallmentAmount(new BigDecimal("15000.00"));
            bReq.setNumberOfInstallments(3);
            bReq.setFirstDueDate(new Date());
            bReq.setFrequency("MONTHLY");

            AbizContract contractB = contractService.createContract(bReq);
            contractBId = contractB.getId();
            InstallmentScheduleResponse bSched = installmentService.generateSchedule(contractBId);
            installmentBId = bSched.getInstallments().get(0).getId();
        } finally {
            AbizTenantContext.clear();
        }

        // 2. Shop Owner A cross-tenant access checks
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // GET Shop B contract schedule
            boolean schedBlocked = false;
            try {
                installmentService.getContractSchedule(contractBId);
            } catch (SecurityException e) {
                schedBlocked = true;
            }
            assertCondition(schedBlocked, "Shop Owner A MUST be blocked from viewing Shop B contract schedule");

            // GET Shop B installment by ID
            boolean instBlocked = false;
            try {
                installmentService.getInstallmentById(installmentBId);
            } catch (SecurityException e) {
                instBlocked = true;
            }
            assertCondition(instBlocked, "Shop Owner A MUST be blocked from viewing Shop B installment by ID");

            // Generate schedule for Shop B contract
            boolean genBlocked = false;
            try {
                installmentService.generateSchedule(contractBId);
            } catch (SecurityException e) {
                genBlocked = true;
            }
            assertCondition(genBlocked, "Shop Owner A MUST be blocked from generating schedule for Shop B contract");

            // Search Shop B installments
            boolean searchBlocked = false;
            try {
                installmentService.listInstallments(shopB, null, null, null, 0, 20);
            } catch (SecurityException e) {
                searchBlocked = true;
            }
            assertCondition(searchBlocked, "Shop Owner A supplying Shop B in search query is blocked with SecurityException");

        } finally {
            AbizTenantContext.clear();
        }

        // 3. Shop Staff A checks
        AbizTenantContext.set(staffA, AbizRole.SHOP_STAFF, shopA, "staff.a@test.com");
        try {
            // Shop Staff A can view own shop installments
            List<AbizInstallment> staffView = installmentService.listInstallments(shopA, null, null, null, 0, 10);
            assertCondition(!staffView.isEmpty(), "Shop Staff A is permitted to list own shop installments");

            // Shop Staff A blocked from Shop B installment
            boolean staffCrossBlocked = false;
            try {
                installmentService.getInstallmentById(installmentBId);
            } catch (SecurityException e) {
                staffCrossBlocked = true;
            }
            assertCondition(staffCrossBlocked, "Shop Staff A MUST be blocked from viewing Shop B installment");

        } finally {
            AbizTenantContext.clear();
        }

        // 4. Super Admin platform-wide checks
        AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            InstallmentScheduleResponse adminView = installmentService.getContractSchedule(contractBId);
            assertCondition(adminView != null && adminView.getInstallments().size() == 3,
                    "Super Admin can inspect contract schedule in any shop");

            AbizInstallment adminInst = installmentService.getInstallmentById(installmentBId);
            assertCondition(adminInst != null, "Super Admin can inspect single installment in any shop");

            List<AbizInstallment> allInst = installmentService.listInstallments(null, null, null, null, 0, 50);
            assertCondition(allInst.size() >= 10, "Super Admin can search installments across all shops");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testAuditTrailVerification(SqlSession session) {
        System.out.println("\n--- Test Suite 12: Audit Trail Generation for Installment Events ---");
        Connection conn = session.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT action, result, count(1) AS cnt FROM abiz_audit_logs " +
                "WHERE action IN ('INSTALLMENT_SCHEDULE_GENERATED', 'CROSS_TENANT_INSTALLMENT_ACCESS_BLOCKED') " +
                "GROUP BY action, result")) {
            ResultSet rs = ps.executeQuery();
            int totalLogged = 0;
            while (rs.next()) {
                String action = rs.getString("action");
                String result = rs.getString("result");
                int count = rs.getInt("cnt");
                totalLogged += count;
                System.out.println(String.format("  [AUDIT LOGGED] action=%s, result=%s, count=%d", action, result, count));
            }
            assertCondition(totalLogged > 0, "Installment operational events recorded in abiz_audit_logs");
        } catch (Exception e) {
            assertCondition(false, "Failed to query abiz_audit_logs: " + e.getMessage());
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
