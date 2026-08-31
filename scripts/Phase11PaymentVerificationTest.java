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
import com.abiz.locker.payment.PaymentService;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Automated Verification Test Suite for Phase 11 — Payment Processing & Allocation.
 * Executes against real PostgreSQL 18.0 database covering financial validation,
 * targeted & waterfall allocations, lifecycle, idempotency, RBAC, tenant isolation,
 * multi-threaded concurrency race conditions, and audit logging.
 */
public class Phase11PaymentVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static long imeiSequence = 86000000000000L + (System.currentTimeMillis() % 10000000000L);

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 11 PAYMENT PROCESSING TEST SUITE");
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
            config.addMapper(ShopUserMapper.class);
            config.addMapper(AuditMapper.class);
            config.addMapper(CustomerMapper.class);
            config.addMapper(FinancedDeviceMapper.class);
            config.addMapper(ContractMapper.class);
            config.addMapper(InstallmentMapper.class);
            config.addMapper(PaymentMapper.class);

            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = factory.openSession(true)) {
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                ShopUserMapper shopUserMapper = session.getMapper(ShopUserMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);
                CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);
                FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);
                ContractMapper contractMapper = session.getMapper(ContractMapper.class);
                InstallmentMapper installmentMapper = session.getMapper(InstallmentMapper.class);
                PaymentMapper paymentMapper = session.getMapper(PaymentMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                Field auditMapperField = AbizAuditService.class.getDeclaredField("auditMapper");
                auditMapperField.setAccessible(true);
                auditMapperField.set(auditService, auditMapper);

                ShopService shopService = new ShopService(shopMapper, auditService);
                CustomerService customerService = new CustomerService(customerMapper, shopMapper, auditService);
                FinancedDeviceService deviceService = new FinancedDeviceService(deviceMapper, customerMapper, auditService);
                ContractService contractService = new ContractService(contractMapper, customerMapper, deviceMapper, auditService);
                InstallmentService installmentService = new InstallmentService(installmentMapper, contractMapper, auditService);
                PaymentService paymentService = new PaymentService(paymentMapper, installmentMapper, auditService);

                UUID superAdminId = UUID.randomUUID();
                AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");

                // Seed Test Shops
                AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                        "Pay Shop A " + UUID.randomUUID().toString().substring(0, 5),
                        "Owner A", "+923007771111", "payA@test.com", "Addr", "Lahore", "ACTIVE"
                ));
                AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                        "Pay Shop B " + UUID.randomUUID().toString().substring(0, 5),
                        "Owner B", "+923007772222", "payB@test.com", "Addr", "Karachi", "ACTIVE"
                ));
                UUID shopAId = shopA.getId();
                UUID shopBId = shopB.getId();

                // Seed Shop Users for Shop A and Shop B
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

                // Seed Customers
                AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                AbizCustomer custA = customerService.createCustomer(new CustomerCreateRequest(
                        "Cust Pay A", "+923003331111", "35201-3331111-1", "Lahore"
                ));
                AbizTenantContext.set(ownerBId, AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
                AbizCustomer custB = customerService.createCustomer(new CustomerCreateRequest(
                        "Cust Pay B", "+923003332222", "35201-3332222-1", "Karachi"
                ));

                // Seed Devices
                AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                AbizFinancedDevice devA1 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A14", "Samsung"));
                AbizFinancedDevice devA2 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A34", "Samsung"));
                AbizFinancedDevice devA3 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A54", "Samsung"));

                AbizTenantContext.set(ownerBId, AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
                AbizFinancedDevice devB1 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Vivo Y27", "Vivo"));

                AbizTenantContext.clear();

                // Run All Phase 11 Test Suites
                testValidationAndContractEligibility(contractService, paymentService, shopAId, ownerAId, custA.getId(), devA1.getId());
                testExactInstallmentPayment(contractService, installmentService, paymentService, shopAId, ownerAId, custA.getId(), devA2.getId());
                testPartialInstallmentPayment(contractService, installmentService, paymentService, deviceService, shopAId, ownerAId, custA.getId());
                testTargetedInstallmentOverageCascade(contractService, installmentService, paymentService, deviceService, shopAId, ownerAId, custA.getId());
                testContractLevelChronologicalWaterfall(contractService, installmentService, paymentService, deviceService, shopAId, ownerAId, custA.getId());
                testContractFullPayoffAndCompletion(contractService, installmentService, paymentService, shopAId, ownerAId, custA.getId(), devA3.getId());
                testPaymentReferenceIdempotency(contractService, installmentService, paymentService, deviceService, shopAId, ownerAId, custA.getId());
                testTenantIsolationAndRbac(contractService, installmentService, paymentService, shopAId, shopBId, ownerAId, staffAId, ownerBId, superAdminId, custA.getId(), custB.getId(), devB1.getId());
                testConcurrentPaymentsRaceCondition(factory, contractService, installmentService, deviceService, shopAId, ownerAId, custA.getId());
                testAuditTrailLogging(session);

                System.out.println("\n===============================================================");
                System.out.println(String.format("PHASE 11 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
                System.out.println("===============================================================");

                if (testsFailed > 0) {
                    System.exit(1);
                }
            }

        } catch (Exception e) {
            System.err.println("FATAL PHASE 11 TEST SUITE FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testValidationAndContractEligibility(
            ContractService contractService, PaymentService paymentService,
            UUID shopAId, UUID ownerAId, UUID custAId, UUID devAId) {
        System.out.println("\n--- Test Suite 1: Financial Validation & Contract Eligibility ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            // 1. Create a DRAFT contract
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(devAId);
            req.setDevicePrice(new BigDecimal("30000.00"));
            req.setDownPayment(new BigDecimal("5000.00"));
            req.setFinancedAmount(new BigDecimal("25000.00"));
            req.setInstallmentAmount(new BigDecimal("5000.00"));
            req.setNumberOfInstallments(5);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract draftContract = contractService.createContract(req);
            UUID draftContractId = draftContract.getId();

            // 2. Reject zero payment
            boolean zeroBlocked = false;
            try {
                paymentService.recordPayment(new PaymentRecordRequest(draftContractId, null, BigDecimal.ZERO, "CASH", null, null));
            } catch (IllegalArgumentException e) {
                zeroBlocked = true;
            }
            assertCondition(zeroBlocked, "Zero payment amount MUST be rejected with IllegalArgumentException");

            // 3. Reject negative payment
            boolean negBlocked = false;
            try {
                paymentService.recordPayment(new PaymentRecordRequest(draftContractId, null, new BigDecimal("-50.00"), "CASH", null, null));
            } catch (IllegalArgumentException e) {
                negBlocked = true;
            }
            assertCondition(negBlocked, "Negative payment amount MUST be rejected with IllegalArgumentException");

            // 4. Reject payment on DRAFT contract
            boolean draftBlocked = false;
            try {
                paymentService.recordPayment(new PaymentRecordRequest(draftContractId, null, new BigDecimal("5000.00"), "CASH", null, null));
            } catch (IllegalStateException e) {
                draftBlocked = true;
            }
            assertCondition(draftBlocked, "Payment on DRAFT contract MUST be rejected with IllegalStateException");

            // 5. Reject payment on CANCELLED contract
            contractService.cancelContract(draftContractId, "Customer cancelled contract");
            boolean cancelledBlocked = false;
            try {
                paymentService.recordPayment(new PaymentRecordRequest(draftContractId, null, new BigDecimal("5000.00"), "CASH", null, null));
            } catch (IllegalStateException e) {
                cancelledBlocked = true;
            }
            assertCondition(cancelledBlocked, "Payment on CANCELLED contract MUST be rejected with IllegalStateException");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testExactInstallmentPayment(
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, UUID shopAId, UUID ownerAId, UUID custAId, UUID devAId) {
        System.out.println("\n--- Test Suite 2: Exact Single Installment Payment ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(devAId);
            req.setDevicePrice(new BigDecimal("40000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("40000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(4);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            contractService.activateContract(contract.getId());
            InstallmentScheduleResponse sched = installmentService.generateSchedule(contract.getId());
            AbizInstallment inst1 = sched.getInstallments().get(0);

            // Record exact payment for installment 1 (10,000.00)
            PaymentRecordResponse resp = paymentService.recordPayment(new PaymentRecordRequest(
                    contract.getId(), inst1.getId(), new BigDecimal("10000.00"), "CASH", "REC-001", "Exact payment"));

            assertCondition(resp != null, "Payment successfully recorded");
            assertCondition(new BigDecimal("30000.00").compareTo(resp.getNewOutstandingBalance()) == 0,
                    "Contract balance reduced from 40000.00 to 30000.00 (Got: " + resp.getNewOutstandingBalance() + ")");

            // Verify installment 1 status and balances
            AbizInstallment updatedInst1 = installmentService.getInstallmentById(inst1.getId());
            assertCondition("PAID".equals(updatedInst1.getStatus()), "Installment 1 status transitioned to PAID");
            assertCondition(BigDecimal.ZERO.compareTo(updatedInst1.getRemainingAmount()) == 0, "Installment 1 remaining_amount is 0.00");
            assertCondition(new BigDecimal("10000.00").compareTo(updatedInst1.getPaidAmount()) == 0, "Installment 1 paid_amount is 10000.00");
            assertCondition(updatedInst1.getPaidAt() != null, "Installment 1 paid_at timestamp is set");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testPartialInstallmentPayment(
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, FinancedDeviceService deviceService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 3: Partial Installment Payment ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Model Partial", "Brand"));
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(dev.getId());
            req.setDevicePrice(new BigDecimal("20000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("20000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(2);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            contractService.activateContract(contract.getId());
            InstallmentScheduleResponse sched = installmentService.generateSchedule(contract.getId());
            AbizInstallment inst1 = sched.getInstallments().get(0);

            // Record partial payment of 4,000.00 on 10,000.00 installment
            PaymentRecordResponse resp = paymentService.recordPayment(new PaymentRecordRequest(
                    contract.getId(), inst1.getId(), new BigDecimal("4000.00"), "BANK", "BANK-PARTIAL-1", "Partial payment"));

            assertCondition(new BigDecimal("16000.00").compareTo(resp.getNewOutstandingBalance()) == 0,
                    "Contract balance reduced to 16000.00 (Got: " + resp.getNewOutstandingBalance() + ")");

            AbizInstallment updatedInst1 = installmentService.getInstallmentById(inst1.getId());
            assertCondition("PARTIALLY_PAID".equals(updatedInst1.getStatus()), "Installment status transitioned to PARTIALLY_PAID");
            assertCondition(new BigDecimal("4000.00").compareTo(updatedInst1.getPaidAmount()) == 0, "Installment paid_amount is 4000.00");
            assertCondition(new BigDecimal("6000.00").compareTo(updatedInst1.getRemainingAmount()) == 0, "Installment remaining_amount is 6000.00");

            // Complete the remaining 6,000.00
            paymentService.recordPayment(new PaymentRecordRequest(
                    contract.getId(), inst1.getId(), new BigDecimal("6000.00"), "CASH", "CASH-COMPL-1", "Complete installment"));

            AbizInstallment fullyPaidInst1 = installmentService.getInstallmentById(inst1.getId());
            assertCondition("PAID".equals(fullyPaidInst1.getStatus()), "Subsequent payment completed installment to PAID");
            assertCondition(BigDecimal.ZERO.compareTo(fullyPaidInst1.getRemainingAmount()) == 0, "Installment remaining_amount is 0.00");
            assertCondition(new BigDecimal("10000.00").compareTo(fullyPaidInst1.getPaidAmount()) == 0, "Installment paid_amount is 10000.00");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testTargetedInstallmentOverageCascade(
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, FinancedDeviceService deviceService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 4: Targeted Installment Overage Cascade to Next Installment ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Model Cascade", "Brand"));
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(dev.getId());
            req.setDevicePrice(new BigDecimal("30000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("30000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(3);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            contractService.activateContract(contract.getId());
            InstallmentScheduleResponse sched = installmentService.generateSchedule(contract.getId());
            AbizInstallment inst1 = sched.getInstallments().get(0);
            AbizInstallment inst2 = sched.getInstallments().get(1);

            // Target installment 1 with 15,000.00 (inst 1 needs 10,000.00; excess 5,000.00 should cascade to inst 2)
            PaymentRecordResponse resp = paymentService.recordPayment(new PaymentRecordRequest(
                    contract.getId(), inst1.getId(), new BigDecimal("15000.00"), "ONLINE", "ONL-CAS-1", "Cascade payment"));

            assertCondition(resp.getAllocatedInstallments().size() == 2, "Payment allocated across exactly 2 installments");
            assertCondition(new BigDecimal("15000.00").compareTo(resp.getNewOutstandingBalance()) == 0,
                    "Contract balance reduced to 15000.00");

            AbizInstallment updatedInst1 = installmentService.getInstallmentById(inst1.getId());
            AbizInstallment updatedInst2 = installmentService.getInstallmentById(inst2.getId());

            assertCondition("PAID".equals(updatedInst1.getStatus()), "Target installment 1 fully settled (PAID)");
            assertCondition("PARTIALLY_PAID".equals(updatedInst2.getStatus()), "Next installment 2 absorbed excess and transitioned to PARTIALLY_PAID");
            assertCondition(new BigDecimal("5000.00").compareTo(updatedInst2.getPaidAmount()) == 0, "Installment 2 paid_amount is 5000.00");
            assertCondition(new BigDecimal("5000.00").compareTo(updatedInst2.getRemainingAmount()) == 0, "Installment 2 remaining_amount is 5000.00");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testContractLevelChronologicalWaterfall(
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, FinancedDeviceService deviceService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 5: Contract-Level Chronological Waterfall Allocation ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Model Waterfall", "Brand"));
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(dev.getId());
            req.setDevicePrice(new BigDecimal("40000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("40000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(4);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            contractService.activateContract(contract.getId());
            installmentService.generateSchedule(contract.getId());

            // Pay 25,000.00 without specifying installmentId -> fills inst 1 (10k), inst 2 (10k), inst 3 (5k)
            PaymentRecordResponse resp = paymentService.recordPayment(new PaymentRecordRequest(
                    contract.getId(), null, new BigDecimal("25000.00"), "BANK", "WATERFALL-1", "Waterfall payment"));

            assertCondition(resp.getAllocatedInstallments().size() == 3, "Waterfall payment allocated across 3 installments");
            assertCondition(new BigDecimal("15000.00").compareTo(resp.getNewOutstandingBalance()) == 0,
                    "Contract balance reduced from 40000.00 to 15000.00");

            InstallmentScheduleResponse updatedSched = installmentService.getContractSchedule(contract.getId());
            assertCondition("PAID".equals(updatedSched.getInstallments().get(0).getStatus()), "Installment 1 is PAID");
            assertCondition("PAID".equals(updatedSched.getInstallments().get(1).getStatus()), "Installment 2 is PAID");
            assertCondition("PARTIALLY_PAID".equals(updatedSched.getInstallments().get(2).getStatus()), "Installment 3 is PARTIALLY_PAID");
            assertCondition("UPCOMING".equals(updatedSched.getInstallments().get(3).getStatus()) ||
                    "DUE".equals(updatedSched.getInstallments().get(3).getStatus()), "Installment 4 untouched");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testContractFullPayoffAndCompletion(
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, UUID shopAId, UUID ownerAId, UUID custAId, UUID devAId) {
        System.out.println("\n--- Test Suite 6: Full Contract Payoff & Transition to COMPLETED ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(devAId);
            req.setDevicePrice(new BigDecimal("15000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("15000.00"));
            req.setInstallmentAmount(new BigDecimal("5000.00"));
            req.setNumberOfInstallments(3);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            contractService.activateContract(contract.getId());
            installmentService.generateSchedule(contract.getId());

            // 1. Pay in full (15,000.00)
            PaymentRecordResponse resp = paymentService.recordPayment(new PaymentRecordRequest(
                    contract.getId(), null, new BigDecimal("15000.00"), "CASH", "FULL-PAYOFF", "Full payoff"));

            assertCondition(BigDecimal.ZERO.compareTo(resp.getNewOutstandingBalance()) == 0,
                    "Contract balance is exactly 0.00");
            assertCondition("COMPLETED".equals(resp.getContractStatus()),
                    "Contract status transitioned to COMPLETED (Got: " + resp.getContractStatus() + ")");

            // 2. Reject subsequent payment on already COMPLETED contract
            boolean completedBlocked = false;
            try {
                paymentService.recordPayment(new PaymentRecordRequest(
                        contract.getId(), null, new BigDecimal("100.00"), "CASH", "OVER-PAY", "Post-completion attempt"));
            } catch (IllegalStateException e) {
                completedBlocked = true;
            }
            assertCondition(completedBlocked, "Payment on COMPLETED contract rejected with IllegalStateException");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testPaymentReferenceIdempotency(
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, FinancedDeviceService deviceService,
            UUID shopAId, UUID ownerAId, UUID custAId) {
        System.out.println("\n--- Test Suite 7: Payment-Reference Idempotency ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Model Idemp", "Brand"));
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(dev.getId());
            req.setDevicePrice(new BigDecimal("20000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("20000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(2);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            contractService.activateContract(contract.getId());
            installmentService.generateSchedule(contract.getId());

            String idempotencyKey = "TX-IDEMP-" + UUID.randomUUID().toString().substring(0, 8);

            // First submission
            PaymentRecordResponse resp1 = paymentService.recordPayment(new PaymentRecordRequest(
                    contract.getId(), null, new BigDecimal("5000.00"), "ONLINE", idempotencyKey, "First submit"));
            assertCondition(new BigDecimal("15000.00").compareTo(resp1.getNewOutstandingBalance()) == 0,
                    "First payment reduced balance to 15000.00");

            // Duplicate submission with identical reference
            PaymentRecordResponse resp2 = paymentService.recordPayment(new PaymentRecordRequest(
                    contract.getId(), null, new BigDecimal("5000.00"), "ONLINE", idempotencyKey, "Duplicate submit"));

            assertCondition(resp1.getPayment().getId().equals(resp2.getPayment().getId()),
                    "Duplicate submission returned IDENTICAL payment receipt ID (Idempotent)");
            assertCondition(new BigDecimal("15000.00").compareTo(resp2.getNewOutstandingBalance()) == 0,
                    "Contract balance was NOT double-decreased (Remained 15000.00)");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testTenantIsolationAndRbac(
            ContractService contractService, InstallmentService installmentService,
            PaymentService paymentService, UUID shopAId, UUID shopBId, UUID ownerAId,
            UUID staffAId, UUID ownerBId, UUID superAdminId, UUID custAId, UUID custBId, UUID devBId) {
        System.out.println("\n--- Test Suite 8: Strict Tenant Isolation & RBAC ---");

        // 1. Create and activate contract in Shop B
        UUID contractBId;
        UUID paymentBId;
        AbizTenantContext.set(ownerBId, AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
        try {
            ContractCreateRequest bReq = new ContractCreateRequest();
            bReq.setCustomerId(custBId);
            bReq.setFinancedDeviceId(devBId);
            bReq.setDevicePrice(new BigDecimal("50000.00"));
            bReq.setDownPayment(BigDecimal.ZERO);
            bReq.setFinancedAmount(new BigDecimal("50000.00"));
            bReq.setInstallmentAmount(new BigDecimal("25000.00"));
            bReq.setNumberOfInstallments(2);
            bReq.setFirstDueDate(new Date());
            bReq.setFrequency("MONTHLY");

            AbizContract contractB = contractService.createContract(bReq);
            contractBId = contractB.getId();
            contractService.activateContract(contractBId);
            installmentService.generateSchedule(contractBId);

            PaymentRecordResponse bPay = paymentService.recordPayment(new PaymentRecordRequest(
                    contractBId, null, new BigDecimal("10000.00"), "CASH", "SHOP-B-PAY", "Shop B payment"));
            paymentBId = bPay.getPayment().getId();
        } finally {
            AbizTenantContext.clear();
        }

        // 2. Shop Owner A cross-tenant access checks
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        try {
            // Record payment for Shop B contract -> BLOCKED
            boolean payBlocked = false;
            try {
                paymentService.recordPayment(new PaymentRecordRequest(
                        contractBId, null, new BigDecimal("5000.00"), "CASH", "ILLEGAL-PAY", "Cross shop pay"));
            } catch (SecurityException e) {
                payBlocked = true;
            }
            assertCondition(payBlocked, "Shop Owner A MUST be blocked from recording payment for Shop B contract");

            // View Shop B payment receipt -> BLOCKED
            boolean viewBlocked = false;
            try {
                paymentService.getPaymentById(paymentBId);
            } catch (SecurityException e) {
                viewBlocked = true;
            }
            assertCondition(viewBlocked, "Shop Owner A MUST be blocked from viewing Shop B payment receipt");

            // List Shop B contract payments -> BLOCKED
            boolean listBlocked = false;
            try {
                paymentService.getPaymentsByContract(contractBId);
            } catch (SecurityException e) {
                listBlocked = true;
            }
            assertCondition(listBlocked, "Shop Owner A MUST be blocked from listing Shop B contract payments");

        } finally {
            AbizTenantContext.clear();
        }

        // 3. Shop Staff A checks
        AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopAId, "staff.a@test.com");
        try {
            // Staff can view own shop payments
            List<AbizPayment> staffView = paymentService.listPayments(shopAId, null, null, null, 0, 10);
            assertCondition(!staffView.isEmpty(), "Shop Staff A is permitted to list own shop payments");

            // Staff cross-tenant blocked
            boolean staffBlocked = false;
            try {
                paymentService.getPaymentById(paymentBId);
            } catch (SecurityException e) {
                staffBlocked = true;
            }
            assertCondition(staffBlocked, "Shop Staff A MUST be blocked from viewing Shop B payment");

        } finally {
            AbizTenantContext.clear();
        }

        // 4. Super Admin platform-wide checks
        AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            AbizPayment adminView = paymentService.getPaymentById(paymentBId);
            assertCondition(adminView != null && adminView.getId().equals(paymentBId),
                    "Super Admin can inspect payment receipt across shops");

            List<AbizPayment> allPayments = paymentService.listPayments(null, null, null, null, 0, 50);
            assertCondition(!allPayments.isEmpty(), "Super Admin can search payments across all shops");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testConcurrentPaymentsRaceCondition(
            SqlSessionFactory factory, ContractService contractService,
            InstallmentService installmentService, FinancedDeviceService deviceService,
            UUID shopAId, UUID ownerAId, UUID custAId) throws Exception {
        System.out.println("\n--- Test Suite 9: Multi-Threaded Concurrency (10 Threads Final-Balance Race) ---");

        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
        final UUID targetContractId;
        try {
            AbizFinancedDevice dev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Model Conc", "Brand"));
            // Contract for 10,000.00 total financed amount with 5 installments of 2,000.00
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(dev.getId());
            req.setDevicePrice(new BigDecimal("10000.00"));
            req.setDownPayment(BigDecimal.ZERO);
            req.setFinancedAmount(new BigDecimal("10000.00"));
            req.setInstallmentAmount(new BigDecimal("2000.00"));
            req.setNumberOfInstallments(5);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            targetContractId = contract.getId();
            contractService.activateContract(targetContractId);
            installmentService.generateSchedule(targetContractId);
        } finally {
            AbizTenantContext.clear();
        }

        // 10 concurrent threads each attempt to pay 2,000.00 simultaneously
        // Only 5 can succeed (5 * 2,000 = 10,000.00). The remaining 5 MUST be rejected as overpayment!
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
                    startLatch.await(); // Synchronized release of all 10 threads

                    AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner" + tIndex + "@test.com");

                    try (SqlSession threadSession = factory.openSession(false)) {
                        PaymentMapper threadPayMapper = threadSession.getMapper(PaymentMapper.class);
                        InstallmentMapper threadInstMapper = threadSession.getMapper(InstallmentMapper.class);
                        AuditMapper threadAuditMapper = threadSession.getMapper(AuditMapper.class);

                        AbizAuditService threadAudit = new AbizAuditService();
                        Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                        f.setAccessible(true);
                        f.set(threadAudit, threadAuditMapper);

                        PaymentService threadPayService = new PaymentService(threadPayMapper, threadInstMapper, threadAudit);

                        threadPayService.recordPayment(new PaymentRecordRequest(
                                targetContractId, null, new BigDecimal("2000.00"), "CASH", "CONC-PAY-" + tIndex, "Concurrent pay"));
                        threadSession.commit();
                        successCount.incrementAndGet();
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    // Expected overpayment or completed rejection for threads that arrive after balance is exhausted
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Thread " + tIndex + " unexpected error: " + e.getMessage());
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

        System.out.println(String.format("  [CONCURRENCY METRICS] 10 concurrent threads: %d succeeded (10,000.00 collected), %d cleanly rejected (overpayment blocked)",
                successCount.get(), rejectedCount.get()));

        assertCondition(successCount.get() == 5, "EXACTLY 5 threads succeeded in paying the 10,000.00 balance (Found: " + successCount.get() + ")");
        assertCondition(rejectedCount.get() == 5, "EXACTLY 5 threads were safely rejected as overpayments (Found: " + rejectedCount.get() + ")");

        // Direct SQL checks in PostgreSQL
        try (SqlSession verifySession = factory.openSession(true)) {
            Connection conn = verifySession.getConnection();

            // 1. Contract balance must be exactly 0.00 and status COMPLETED
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT outstanding_balance, status FROM abiz_contracts WHERE id = ?")) {
                ps.setObject(1, targetContractId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    BigDecimal dbBalance = rs.getBigDecimal("outstanding_balance");
                    String dbStatus = rs.getString("status");
                    assertCondition(BigDecimal.ZERO.compareTo(dbBalance) == 0,
                            "PostgreSQL contract outstanding_balance is EXACTLY 0.00 (No negative balance! Got: " + dbBalance + ")");
                    assertCondition("COMPLETED".equals(dbStatus),
                            "PostgreSQL contract status is COMPLETED (Got: " + dbStatus + ")");
                }
            }

            // 2. All 5 installments must be PAID with remaining_amount = 0.00
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(1) FROM abiz_installments WHERE contract_id = ? AND status = 'PAID' AND remaining_amount = 0")) {
                ps.setObject(1, targetContractId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int paidCount = rs.getInt(1);
                    assertCondition(paidCount == 5, "All 5 installments in PostgreSQL are PAID with 0 remaining amount");
                }
            }

            // 3. Exactly 5 payments committed in abiz_payments
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(1), SUM(amount) FROM abiz_payments WHERE contract_id = ?")) {
                ps.setObject(1, targetContractId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int payCount = rs.getInt(1);
                    BigDecimal totalPaid = rs.getBigDecimal(2);
                    assertCondition(payCount == 5, "PostgreSQL contains EXACTLY 5 payment records (Found: " + payCount + ")");
                    assertCondition(new BigDecimal("10000.00").compareTo(totalPaid) == 0,
                            "SUM(payments) in PostgreSQL matches EXACTLY 10000.00 (Got: " + totalPaid + ")");
                }
            }
        }
    }

    private static void testAuditTrailLogging(SqlSession session) {
        System.out.println("\n--- Test Suite 10: Audit Trail Generation for Payment Events ---");
        Connection conn = session.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT action, result, count(1) AS cnt FROM abiz_audit_logs " +
                "WHERE action IN ('PAYMENT_RECORDED', 'CONTRACT_COMPLETED', 'CROSS_TENANT_PAYMENT_ACCESS_BLOCKED', 'PAYMENT_REJECTED') " +
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
            assertCondition(totalLogged > 0, "Payment operational events recorded in abiz_audit_logs");
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
