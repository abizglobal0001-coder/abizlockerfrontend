package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.contract.ContractService;
import com.abiz.locker.customer.CustomerService;
import com.abiz.locker.device.FinancedDeviceService;
import com.abiz.locker.domain.*;
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
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Automated Verification Suite for Phase 9 — Installment Contracts.
 * Executes live against the local PostgreSQL 18.0 database.
 */
public class Phase9ContractVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static long imeiSequence = 86000000000000L + (System.currentTimeMillis() % 10000000000L);

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 9 INSTALLMENT CONTRACTS TEST SUITE");
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

            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = factory.openSession(true)) {
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);
                CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);
                FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);
                ContractMapper contractMapper = session.getMapper(ContractMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                Field auditMapperField = AbizAuditService.class.getDeclaredField("auditMapper");
                auditMapperField.setAccessible(true);
                auditMapperField.set(auditService, auditMapper);

                ShopService shopService = new ShopService(shopMapper, auditService);
                CustomerService customerService = new CustomerService(customerMapper, shopMapper, auditService);
                FinancedDeviceService deviceService = new FinancedDeviceService(deviceMapper, customerMapper, auditService);
                ContractService contractService = new ContractService(contractMapper, customerMapper, deviceMapper, auditService);

                UUID superAdminId = UUID.randomUUID();
                AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");

                // Seed Test Shops
                AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                        "Contract Shop A " + UUID.randomUUID().toString().substring(0, 5),
                        "Owner A", "+923007771111", "cntA@test.com", "Addr", "Lahore", "ACTIVE"
                ));
                AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                        "Contract Shop B " + UUID.randomUUID().toString().substring(0, 5),
                        "Owner B", "+923007772222", "cntB@test.com", "Addr", "Karachi", "ACTIVE"
                ));
                UUID shopAId = shopA.getId();
                UUID shopBId = shopB.getId();

                // Seed Customer in Shop A and Shop B
                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                AbizCustomer custA = customerService.createCustomer(new CustomerCreateRequest(
                        "Cust A", "+923001119999", "35201-1119999-1", "Lahore"
                ));
                AbizCustomer custAInactive = customerService.createCustomer(new CustomerCreateRequest(
                        "Cust A Inactive", "+923001118888", "35201-1118888-1", "Lahore"
                ));
                customerService.updateCustomerStatus(custAInactive.getId(), "INACTIVE");

                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
                AbizCustomer custB = customerService.createCustomer(new CustomerCreateRequest(
                        "Cust B", "+923002229999", "35201-2229999-1", "Karachi"
                ));

                // Seed Financed Devices in Shop A and Shop B
                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                AbizFinancedDevice devA1 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy S23", "Samsung"));
                AbizFinancedDevice devA2 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Galaxy A34", "Samsung"));

                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
                AbizFinancedDevice devB1 = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Xiaomi 13", "Xiaomi"));

                AbizTenantContext.clear();

                UUID ownerAId = UUID.randomUUID();
                UUID staffAId = UUID.randomUUID();

                // Run Test Suites
                testFinancialMathValidations(contractService, shopAId, ownerAId, custA.getId());
                testTripartiteTenantConsistency(contractService, shopAId, ownerAId, custA.getId(), custAInactive.getId(), custB.getId(), devA1.getId(), devB1.getId());
                testContractLifecycleAndImmutability(contractService, deviceMapper, shopAId, ownerAId, custA.getId(), devA1.getId(), devA2.getId());
                testDuplicateActiveDeviceBinding(contractService, shopAId, ownerAId, custA.getId(), devA1.getId());
                testContractSearchAndPagination(contractService, shopAId, ownerAId, custA.getId());
                testTenantIsolationAndRbac(contractService, shopAId, shopBId, ownerAId, staffAId, superAdminId, custA.getId(), custB.getId(), devB1.getId());
                testAuditTrailVerification(session);

                System.out.println("\n===============================================================");
                System.out.println(String.format("PHASE 9 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
                System.out.println("===============================================================");

                if (testsFailed > 0) {
                    System.exit(1);
                }
            }

        } catch (Exception e) {
            System.err.println("FATAL TEST SUITE FAILURE: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testFinancialMathValidations(
            ContractService contractService, UUID shopA, UUID ownerA, UUID custAId) {
        System.out.println("\n--- Test Suite 1: Financial Invariants & BigDecimal Math ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // 1. Valid contract creation
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setDevicePrice(new BigDecimal("120000.00"));
            req.setDownPayment(new BigDecimal("20000.00"));
            req.setFinancedAmount(new BigDecimal("100000.00"));
            req.setInstallmentAmount(new BigDecimal("10000.00"));
            req.setNumberOfInstallments(10);
            req.setFirstDueDate(new Date());
            req.setFrequency("MONTHLY");

            AbizContract contract = contractService.createContract(req);
            assertCondition(contract != null && contract.getId() != null, "Valid contract created");
            assertCondition("DRAFT".equals(contract.getStatus()), "Initial status must be DRAFT");
            assertCondition(new BigDecimal("100000.00").compareTo(contract.getOutstandingBalance()) == 0,
                    "Outstanding balance initialized to financed amount");

            // 2. Negative device price
            boolean negPriceRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(req);
                badReq.setDevicePrice(new BigDecimal("-100.00"));
                contractService.createContract(badReq);
            } catch (IllegalArgumentException e) {
                negPriceRejected = true;
            }
            assertCondition(negPriceRejected, "Negative devicePrice rejected");

            // 3. Negative down payment
            boolean negDownRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(req);
                badReq.setDownPayment(new BigDecimal("-50.00"));
                contractService.createContract(badReq);
            } catch (IllegalArgumentException e) {
                negDownRejected = true;
            }
            assertCondition(negDownRejected, "Negative downPayment rejected");

            // 4. Down payment >= device price
            boolean downGtePriceRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(req);
                badReq.setDownPayment(new BigDecimal("120000.00"));
                contractService.createContract(badReq);
            } catch (IllegalArgumentException e) {
                downGtePriceRejected = true;
            }
            assertCondition(downGtePriceRejected, "downPayment >= devicePrice rejected");

            // 5. Financed amount mismatch
            boolean financedMismatchRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(req);
                badReq.setFinancedAmount(new BigDecimal("95000.00")); // expected 100000.00
                contractService.createContract(badReq);
            } catch (IllegalArgumentException e) {
                financedMismatchRejected = true;
            }
            assertCondition(financedMismatchRejected, "Financed amount mismatch rejected");

            // 6. Zero installments
            boolean zeroInstallmentsRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(req);
                badReq.setNumberOfInstallments(0);
                contractService.createContract(badReq);
            } catch (IllegalArgumentException e) {
                zeroInstallmentsRejected = true;
            }
            assertCondition(zeroInstallmentsRejected, "Zero numberOfInstallments rejected");

            // 7. Invalid frequency
            boolean invalidFreqRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(req);
                badReq.setFrequency("DAILY");
                contractService.createContract(badReq);
            } catch (IllegalArgumentException e) {
                invalidFreqRejected = true;
            }
            assertCondition(invalidFreqRejected, "Invalid frequency string rejected");

            // 8. Negative grace period
            boolean negGraceRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(req);
                badReq.setGracePeriodDays(-1);
                contractService.createContract(badReq);
            } catch (IllegalArgumentException e) {
                negGraceRejected = true;
            }
            assertCondition(negGraceRejected, "Negative gracePeriodDays rejected");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testTripartiteTenantConsistency(
            ContractService contractService, UUID shopA, UUID ownerA,
            UUID custAId, UUID custAInactiveId, UUID custBId, UUID devAId, UUID devBId) {
        System.out.println("\n--- Test Suite 2: Tripartite Tenant Consistency & Customer/Device Rules ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            ContractCreateRequest baseReq = new ContractCreateRequest();
            baseReq.setDevicePrice(new BigDecimal("80000.00"));
            baseReq.setDownPayment(new BigDecimal("10000.00"));
            baseReq.setFinancedAmount(new BigDecimal("70000.00"));
            baseReq.setInstallmentAmount(new BigDecimal("7000.00"));
            baseReq.setNumberOfInstallments(10);
            baseReq.setFirstDueDate(new Date());

            // 1. Same-shop customer & device assignment succeeds
            baseReq.setCustomerId(custAId);
            baseReq.setFinancedDeviceId(devAId);
            AbizContract validContract = contractService.createContract(baseReq);
            assertCondition(validContract != null && devAId.equals(validContract.getFinancedDeviceId()),
                    "Same-shop customer and device assignment succeeds");

            // 2. Cross-tenant customer: Shop A contract with Shop B customer
            boolean crossCustRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(baseReq);
                badReq.setCustomerId(custBId); // Shop B customer
                contractService.createContract(badReq);
            } catch (SecurityException e) {
                crossCustRejected = true;
            }
            assertCondition(crossCustRejected, "Shop A contract with Shop B customer rejected with SecurityException");

            // 3. Inactive customer rejected
            boolean inactiveCustRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(baseReq);
                badReq.setCustomerId(custAInactiveId); // INACTIVE customer
                contractService.createContract(badReq);
            } catch (IllegalArgumentException e) {
                inactiveCustRejected = true;
            }
            assertCondition(inactiveCustRejected, "INACTIVE customer cannot enter into contracts");

            // 4. Cross-tenant device: Shop A contract with Shop B device
            boolean crossDevRejected = false;
            try {
                ContractCreateRequest badReq = cloneRequest(baseReq);
                badReq.setCustomerId(custAId);
                badReq.setFinancedDeviceId(devBId); // Shop B device
                contractService.createContract(badReq);
            } catch (SecurityException e) {
                crossDevRejected = true;
            }
            assertCondition(crossDevRejected, "Shop A contract with Shop B device rejected with SecurityException");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testContractLifecycleAndImmutability(
            ContractService contractService, FinancedDeviceMapper deviceMapper,
            UUID shopA, UUID ownerA, UUID custAId, UUID devA1Id, UUID devA2Id) {
        System.out.println("\n--- Test Suite 3: Contract Lifecycle Transitions & Immutability ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            ContractCreateRequest req = new ContractCreateRequest();
            req.setCustomerId(custAId);
            req.setFinancedDeviceId(devA2Id);
            req.setDevicePrice(new BigDecimal("60000.00"));
            req.setDownPayment(new BigDecimal("10000.00"));
            req.setFinancedAmount(new BigDecimal("50000.00"));
            req.setInstallmentAmount(new BigDecimal("5000.00"));
            req.setNumberOfInstallments(10);
            req.setFirstDueDate(new Date());

            AbizContract contract = contractService.createContract(req);
            UUID contractId = contract.getId();
            assertCondition("DRAFT".equals(contract.getStatus()), "Contract initially in DRAFT");

            // 1. Update in DRAFT status succeeds
            ContractUpdateRequest updateReq = new ContractUpdateRequest();
            updateReq.setDevicePrice(new BigDecimal("65000.00"));
            updateReq.setDownPayment(new BigDecimal("15000.00"));
            updateReq.setNotes("Updated in DRAFT");
            AbizContract updated = contractService.updateContract(contractId, updateReq);
            assertCondition(new BigDecimal("50000.00").compareTo(updated.getFinancedAmount()) == 0,
                    "Financed amount updated in DRAFT (65000 - 15000 = 50000)");
            assertCondition("Updated in DRAFT".equals(updated.getNotes()), "Notes updated in DRAFT");

            // 2. Activate contract: DRAFT -> ACTIVE
            AbizContract activated = contractService.activateContract(contractId);
            assertCondition("ACTIVE".equals(activated.getStatus()), "Contract activated to ACTIVE");

            // Verify device linkage in abiz_financed_devices
            AbizFinancedDevice linkedDevice = deviceMapper.findDeviceById(devA2Id);
            assertCondition(contractId.equals(linkedDevice.getContractId()),
                    "Device contract_id column updated to match activated contract ID");

            // 3. Immutability: Term updates on ACTIVE contract must be rejected
            boolean activeUpdateRejected = false;
            try {
                ContractUpdateRequest badUpdate = new ContractUpdateRequest();
                badUpdate.setDevicePrice(new BigDecimal("70000.00"));
                contractService.updateContract(contractId, badUpdate);
            } catch (IllegalStateException e) {
                activeUpdateRejected = true;
            }
            assertCondition(activeUpdateRejected, "Financial updates rejected on ACTIVE contract (Terms Immutable)");

            // 4. Repeated activation on ACTIVE contract must be rejected
            boolean reActivateRejected = false;
            try {
                contractService.activateContract(contractId);
            } catch (IllegalStateException e) {
                reActivateRejected = true;
            }
            assertCondition(reActivateRejected, "Re-activation of already ACTIVE contract rejected");

            // 5. Cancel contract: ACTIVE -> CANCELLED
            AbizContract cancelled = contractService.cancelContract(contractId, "Customer decided to cancel");
            assertCondition("CANCELLED".equals(cancelled.getStatus()), "Contract transitioned to CANCELLED");

            // Verify device linkage is released
            AbizFinancedDevice freedDevice = deviceMapper.findDeviceById(devA2Id);
            assertCondition(freedDevice.getContractId() == null,
                    "Device contract_id column set to NULL upon contract cancellation (Device released)");

            // 6. Terminal status cannot be reactivated
            boolean reactivateCancelledRejected = false;
            try {
                contractService.activateContract(contractId);
            } catch (IllegalStateException e) {
                reactivateCancelledRejected = true;
            }
            assertCondition(reactivateCancelledRejected, "CANCELLED contract cannot be reactivated");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testDuplicateActiveDeviceBinding(
            ContractService contractService, UUID shopA, UUID ownerA, UUID custAId, UUID devAId) {
        System.out.println("\n--- Test Suite 4: Duplicate Active Contract on Device Protection ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // Note: devAId was already assigned to an active/draft contract in Test Suite 2
            boolean duplicateDeviceRejected = false;
            try {
                ContractCreateRequest req = new ContractCreateRequest();
                req.setCustomerId(custAId);
                req.setFinancedDeviceId(devAId); // already bound!
                req.setDevicePrice(new BigDecimal("50000.00"));
                req.setDownPayment(new BigDecimal("10000.00"));
                req.setFinancedAmount(new BigDecimal("40000.00"));
                req.setInstallmentAmount(new BigDecimal("4000.00"));
                req.setNumberOfInstallments(10);
                req.setFirstDueDate(new Date());
                contractService.createContract(req);
            } catch (IllegalArgumentException e) {
                duplicateDeviceRejected = true;
            }
            assertCondition(duplicateDeviceRejected,
                    "Binding a device with an existing active/draft contract to a new contract is rejected");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testContractSearchAndPagination(
            ContractService contractService, UUID shopA, UUID ownerA, UUID custAId) {
        System.out.println("\n--- Test Suite 5: Contract Search, Filtering, and Pagination ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            List<AbizContract> contracts = contractService.listContracts(
                    shopA, custAId, null, null, 0, 20
            );
            assertCondition(!contracts.isEmpty(), "Contracts listed for customer in Shop A");

            // Status filter
            List<AbizContract> draftContracts = contractService.listContracts(
                    shopA, null, null, "DRAFT", 0, 20
            );
            for (AbizContract c : draftContracts) {
                assertCondition("DRAFT".equals(c.getStatus()), "Status filter returned only DRAFT contracts");
            }

            // Pagination
            List<AbizContract> paged = contractService.listContracts(
                    shopA, null, null, null, 0, 1
            );
            assertCondition(paged.size() == 1, "Pagination limit=1 returned exactly 1 record");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testTenantIsolationAndRbac(
            ContractService contractService, UUID shopA, UUID shopB,
            UUID ownerA, UUID staffA, UUID superAdminId, UUID custAId, UUID custBId, UUID devBId) {
        System.out.println("\n--- Test Suite 6: RBAC & Strict Tenant Isolation ---");

        // 1. Create a contract in Shop B
        UUID ownerB = UUID.randomUUID();
        AbizContract contractB;
        AbizTenantContext.set(ownerB, AbizRole.SHOP_OWNER, shopB, "owner.b@test.com");
        try {
            ContractCreateRequest bReq = new ContractCreateRequest();
            bReq.setCustomerId(custBId);
            bReq.setFinancedDeviceId(devBId);
            bReq.setDevicePrice(new BigDecimal("90000.00"));
            bReq.setDownPayment(new BigDecimal("10000.00"));
            bReq.setFinancedAmount(new BigDecimal("80000.00"));
            bReq.setInstallmentAmount(new BigDecimal("8000.00"));
            bReq.setNumberOfInstallments(10);
            bReq.setFirstDueDate(new Date());
            contractB = contractService.createContract(bReq);
        } finally {
            AbizTenantContext.clear();
        }

        // 2. Shop Owner A checks
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // View Shop B contract
            boolean crossViewBlocked = false;
            try {
                contractService.getContract(contractB.getId());
            } catch (SecurityException e) {
                crossViewBlocked = true;
            }
            assertCondition(crossViewBlocked, "Shop Owner A MUST be blocked from viewing Shop B contract");

            // Update Shop B contract
            boolean crossUpdateBlocked = false;
            try {
                ContractUpdateRequest uReq = new ContractUpdateRequest();
                uReq.setNotes("Tampered Notes");
                contractService.updateContract(contractB.getId(), uReq);
            } catch (SecurityException e) {
                crossUpdateBlocked = true;
            }
            assertCondition(crossUpdateBlocked, "Shop Owner A MUST be blocked from updating Shop B contract");

            // Activate Shop B contract
            boolean crossActivateBlocked = false;
            try {
                contractService.activateContract(contractB.getId());
            } catch (SecurityException e) {
                crossActivateBlocked = true;
            }
            assertCondition(crossActivateBlocked, "Shop Owner A MUST be blocked from activating Shop B contract");

            // Cancel Shop B contract
            boolean crossCancelBlocked = false;
            try {
                contractService.cancelContract(contractB.getId(), "Cross Cancel");
            } catch (SecurityException e) {
                crossCancelBlocked = true;
            }
            assertCondition(crossCancelBlocked, "Shop Owner A MUST be blocked from cancelling Shop B contract");

            // Parameter tampering
            boolean bodyTamperingBlocked = false;
            try {
                ContractCreateRequest tamperReq = new ContractCreateRequest();
                tamperReq.setShopId(shopB); // attempt to switch shop
                tamperReq.setCustomerId(custAId);
                tamperReq.setDevicePrice(new BigDecimal("50000.00"));
                tamperReq.setDownPayment(BigDecimal.ZERO);
                tamperReq.setFinancedAmount(new BigDecimal("50000.00"));
                tamperReq.setInstallmentAmount(new BigDecimal("5000.00"));
                tamperReq.setNumberOfInstallments(10);
                tamperReq.setFirstDueDate(new Date());
                contractService.createContract(tamperReq);
            } catch (SecurityException e) {
                bodyTamperingBlocked = true;
            }
            assertCondition(bodyTamperingBlocked, "Shop Owner A passing Shop B in request body is rejected");

        } finally {
            AbizTenantContext.clear();
        }

        // 3. Shop Staff A checks
        AbizTenantContext.set(staffA, AbizRole.SHOP_STAFF, shopA, "staff.a@test.com");
        try {
            // Staff A can create contract in Shop A
            ContractCreateRequest staffReq = new ContractCreateRequest();
            staffReq.setCustomerId(custAId);
            staffReq.setDevicePrice(new BigDecimal("40000.00"));
            staffReq.setDownPayment(new BigDecimal("5000.00"));
            staffReq.setFinancedAmount(new BigDecimal("35000.00"));
            staffReq.setInstallmentAmount(new BigDecimal("3500.00"));
            staffReq.setNumberOfInstallments(10);
            staffReq.setFirstDueDate(new Date());
            AbizContract staffContract = contractService.createContract(staffReq);
            assertCondition(staffContract != null && shopA.equals(staffContract.getShopId()),
                    "Shop Staff A is permitted to create contracts in Shop A");

            // Staff A blocked from Shop B contract
            boolean staffCrossBlocked = false;
            try {
                contractService.getContract(contractB.getId());
            } catch (SecurityException e) {
                staffCrossBlocked = true;
            }
            assertCondition(staffCrossBlocked, "Shop Staff A MUST be blocked from viewing Shop B contract");

        } finally {
            AbizTenantContext.clear();
        }

        // 4. Super Admin platform-wide checks
        AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            AbizContract adminViewed = contractService.getContract(contractB.getId());
            assertCondition(adminViewed != null, "Super Admin can view contract in any shop");

            List<AbizContract> allContracts = contractService.listContracts(null, null, null, null, 0, 50);
            assertCondition(allContracts.size() >= 2, "Super Admin can list contracts across all shops");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testAuditTrailVerification(SqlSession session) {
        System.out.println("\n--- Test Suite 7: Audit Trail Generation for Contract Events ---");
        Connection conn = session.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT action, result, count(1) AS cnt FROM abiz_audit_logs " +
                "WHERE action IN ('CONTRACT_CREATED', 'CONTRACT_UPDATED', 'CONTRACT_ACTIVATED', 'CONTRACT_CANCELLED', 'CROSS_TENANT_CONTRACT_ACCESS_BLOCKED') " +
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
            assertCondition(totalLogged > 0, "Contract operational events recorded in abiz_audit_logs");
        } catch (Exception e) {
            assertCondition(false, "Failed to query abiz_audit_logs: " + e.getMessage());
        }
    }

    private static ContractCreateRequest cloneRequest(ContractCreateRequest req) {
        ContractCreateRequest copy = new ContractCreateRequest();
        copy.setShopId(req.getShopId());
        copy.setCustomerId(req.getCustomerId());
        copy.setFinancedDeviceId(req.getFinancedDeviceId());
        copy.setDevicePrice(req.getDevicePrice());
        copy.setDownPayment(req.getDownPayment());
        copy.setFinancedAmount(req.getFinancedAmount());
        copy.setInstallmentAmount(req.getInstallmentAmount());
        copy.setFrequency(req.getFrequency());
        copy.setNumberOfInstallments(req.getNumberOfInstallments());
        copy.setFirstDueDate(req.getFirstDueDate());
        copy.setGracePeriodDays(req.getGracePeriodDays());
        copy.setNotes(req.getNotes());
        return copy;
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
