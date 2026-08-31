package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.customer.CustomerService;
import com.abiz.locker.domain.AbizCustomer;
import com.abiz.locker.domain.AbizShop;
import com.abiz.locker.domain.CustomerCreateRequest;
import com.abiz.locker.domain.CustomerUpdateRequest;
import com.abiz.locker.domain.ShopCreateRequest;
import com.abiz.locker.mapper.AuditMapper;
import com.abiz.locker.mapper.CustomerMapper;
import com.abiz.locker.mapper.ShopMapper;
import com.abiz.locker.mapper.UuidTypeHandler;
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
import java.util.List;
import java.util.UUID;

/**
 * End-to-end Phase 7 Customer Management Automated Verification Test Suite.
 * Executes live against the local PostgreSQL 18.0 database.
 */
public class Phase7CustomerVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 7 CUSTOMER MANAGEMENT TEST SUITE");
        System.out.println("===============================================================");

        try {
            // 1. Initialize Database Session & Mappers
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

            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = factory.openSession(true)) {
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);
                CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                Field auditMapperField = AbizAuditService.class.getDeclaredField("auditMapper");
                auditMapperField.setAccessible(true);
                auditMapperField.set(auditService, auditMapper);

                CustomerService customerService = new CustomerService(customerMapper, shopMapper, auditService);
                ShopService shopService = new ShopService(shopMapper, auditService);

                UUID superAdminId = UUID.randomUUID();
                AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");

                // Seed test shops
                AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                        "Cust Shop A " + UUID.randomUUID().toString().substring(0, 6),
                        "Owner A", "+923001118888", "custA@test.com", "Addr", "Lahore", "ACTIVE"
                ));
                AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                        "Cust Shop B " + UUID.randomUUID().toString().substring(0, 6),
                        "Owner B", "+923002228888", "custB@test.com", "Addr", "Karachi", "ACTIVE"
                ));
                UUID shopAId = shopA.getId();
                UUID shopBId = shopB.getId();
                AbizTenantContext.clear();

                UUID ownerAId = UUID.randomUUID();
                UUID staffAId = UUID.randomUUID();

                // Run Test Suites
                testCustomerCreation(customerService, shopAId, ownerAId);
                testDuplicatePhoneAndCnicPrevention(customerService, shopAId, shopBId, ownerAId);
                testInputValidations(customerService, shopAId, ownerAId);
                testCustomerSearchAndPagination(customerService, shopAId, ownerAId);
                testCustomerUpdates(customerService, shopAId, ownerAId);
                testCustomerStatusTransitions(customerService, shopAId, ownerAId);
                testRbacAndTenantBoundaries(customerService, shopAId, shopBId, ownerAId, staffAId, superAdminId);
                testAuditTrailVerification(session);

                System.out.println("\n===============================================================");
                System.out.println(String.format("PHASE 7 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
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

    private static void testCustomerCreation(CustomerService customerService, UUID shopA, UUID ownerA) {
        System.out.println("\n--- Test Suite 1: Customer Creation ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            CustomerCreateRequest req = new CustomerCreateRequest();
            req.setName("Muhammad Tariq");
            req.setPhone("+923001234567");
            req.setAlternatePhone("+923007654321");
            req.setCnic("35201-1234567-1");
            req.setEmail("tariq@example.com");
            req.setAddress("House 12, Street 5, Gulberg");
            req.setCity("Lahore");
            req.setNotes("Self-employed retail businessman");
            req.setStatus("ACTIVE");

            AbizCustomer created = customerService.createCustomer(req);
            assertCondition(created != null, "Created customer must not be null");
            assertCondition(created.getId() != null, "Customer ID must be populated");
            assertCondition(shopA.equals(created.getShopId()), "Customer shopId must match Shop A");
            assertCondition("Muhammad Tariq".equals(created.getName()), "Customer name must match");
            assertCondition("+923001234567".equals(created.getPhone()), "Customer phone must match");
            assertCondition("35201-1234567-1".equals(created.getCnic()), "Customer CNIC must match");
            assertCondition("ACTIVE".equals(created.getStatus()), "Customer status must be ACTIVE");
            assertCondition(created.getCreatedAt() != null, "createdAt must be populated");
            assertCondition(created.getUpdatedAt() != null, "updatedAt must be populated");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testDuplicatePhoneAndCnicPrevention(CustomerService customerService, UUID shopA, UUID shopB, UUID ownerA) {
        System.out.println("\n--- Test Suite 2: Duplicate Phone & CNIC Prevention ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // Duplicate phone in same shop
            boolean dupPhoneRejected = false;
            try {
                CustomerCreateRequest dupPhone = new CustomerCreateRequest();
                dupPhone.setName("Different Name");
                dupPhone.setPhone("+923001234567"); // Same phone as Tariq
                dupPhone.setCnic("35201-9999999-1");
                customerService.createCustomer(dupPhone);
            } catch (IllegalArgumentException e) {
                dupPhoneRejected = true;
            }
            assertCondition(dupPhoneRejected, "Duplicate phone in the same shop must be rejected");

            // Duplicate CNIC in same shop
            boolean dupCnicRejected = false;
            try {
                CustomerCreateRequest dupCnic = new CustomerCreateRequest();
                dupCnic.setName("Different Name");
                dupCnic.setPhone("+923009998888");
                dupCnic.setCnic("35201-1234567-1"); // Same CNIC as Tariq
                customerService.createCustomer(dupCnic);
            } catch (IllegalArgumentException e) {
                dupCnicRejected = true;
            }
            assertCondition(dupCnicRejected, "Duplicate CNIC in the same shop must be rejected");

        } finally {
            AbizTenantContext.clear();
        }

        // Same phone & CNIC in a DIFFERENT shop (Shop B) should SUCCEED
        UUID ownerB = UUID.randomUUID();
        AbizTenantContext.set(ownerB, AbizRole.SHOP_OWNER, shopB, "owner.b@test.com");
        try {
            CustomerCreateRequest shopBReq = new CustomerCreateRequest();
            shopBReq.setName("Muhammad Tariq in Shop B");
            shopBReq.setPhone("+923001234567"); // Identical phone
            shopBReq.setCnic("35201-1234567-1"); // Identical CNIC
            shopBReq.setCity("Karachi");

            AbizCustomer shopBCustomer = customerService.createCustomer(shopBReq);
            assertCondition(shopBCustomer != null && shopB.equals(shopBCustomer.getShopId()),
                    "Same phone/CNIC in a different shop MUST succeed (tenant isolation)");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testInputValidations(CustomerService customerService, UUID shopA, UUID ownerA) {
        System.out.println("\n--- Test Suite 3: Input Validations ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // Short name
            boolean shortNameRejected = false;
            try {
                CustomerCreateRequest req = new CustomerCreateRequest();
                req.setName("A");
                req.setPhone("+923001112233");
                customerService.createCustomer(req);
            } catch (IllegalArgumentException e) {
                shortNameRejected = true;
            }
            assertCondition(shortNameRejected, "Customer name shorter than 2 chars must be rejected");

            // Short phone
            boolean shortPhoneRejected = false;
            try {
                CustomerCreateRequest req = new CustomerCreateRequest();
                req.setName("Valid Name");
                req.setPhone("123");
                customerService.createCustomer(req);
            } catch (IllegalArgumentException e) {
                shortPhoneRejected = true;
            }
            assertCondition(shortPhoneRejected, "Customer phone shorter than 7 chars must be rejected");

            // Invalid status
            boolean invalidStatusRejected = false;
            try {
                CustomerCreateRequest req = new CustomerCreateRequest();
                req.setName("Valid Name");
                req.setPhone("+923001112233");
                req.setStatus("UNKNOWN_STATUS");
                customerService.createCustomer(req);
            } catch (IllegalArgumentException e) {
                invalidStatusRejected = true;
            }
            assertCondition(invalidStatusRejected, "Invalid status string must be rejected");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCustomerSearchAndPagination(CustomerService customerService, UUID shopA, UUID ownerA) {
        System.out.println("\n--- Test Suite 4: Customer Search & Pagination ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // Seed a few more customers for Shop A
            CustomerCreateRequest c1 = new CustomerCreateRequest("Ali Raza", "+923001110001", "35201-1110001-1", "Lahore");
            CustomerCreateRequest c2 = new CustomerCreateRequest("Bilal Ahmed", "+923001110002", "35201-1110002-1", "Lahore");
            CustomerCreateRequest c3 = new CustomerCreateRequest("Zubair Khan", "+923001110003", "35201-1110003-1", "Lahore");
            customerService.createCustomer(c1);
            customerService.createCustomer(c2);
            customerService.createCustomer(c3);

            // Search by name
            List<AbizCustomer> searchByName = customerService.listCustomers(shopA, "Bilal", null, 0, 10);
            assertCondition(searchByName.size() == 1, "Search by name 'Bilal' must return exactly 1 match");
            assertCondition("Bilal Ahmed".equals(searchByName.get(0).getName()), "Matching customer must be Bilal Ahmed");

            // Search by phone
            List<AbizCustomer> searchByPhone = customerService.listCustomers(shopA, "1110003", null, 0, 10);
            assertCondition(searchByPhone.size() == 1, "Search by phone '1110003' must return exactly 1 match");

            // Search by CNIC
            List<AbizCustomer> searchByCnic = customerService.listCustomers(shopA, "1110001-1", null, 0, 10);
            assertCondition(searchByCnic.size() == 1, "Search by CNIC must return exactly 1 match");

            // Pagination
            List<AbizCustomer> page1 = customerService.listCustomers(shopA, null, null, 0, 2);
            assertCondition(page1.size() == 2, "Page size of 2 must return exactly 2 records");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCustomerUpdates(CustomerService customerService, UUID shopA, UUID ownerA) {
        System.out.println("\n--- Test Suite 5: Customer Updates ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            CustomerCreateRequest req = new CustomerCreateRequest("Hamza Sheikh", "+923005550001", "35201-5550001-1", "Lahore");
            AbizCustomer created = customerService.createCustomer(req);

            // Update details
            CustomerUpdateRequest updateReq = new CustomerUpdateRequest();
            updateReq.setName("Hamza Sheikh Updated");
            updateReq.setAddress("Office 404, Eden Tower");
            updateReq.setCity("Rawalpindi");

            AbizCustomer updated = customerService.updateCustomer(created.getId(), updateReq);
            assertCondition("Hamza Sheikh Updated".equals(updated.getName()), "Updated name must match");
            assertCondition("Office 404, Eden Tower".equals(updated.getAddress()), "Updated address must match");
            assertCondition("Rawalpindi".equals(updated.getCity()), "Updated city must match");
            assertCondition(created.getPhone().equals(updated.getPhone()), "Unmodified phone must remain intact");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCustomerStatusTransitions(CustomerService customerService, UUID shopA, UUID ownerA) {
        System.out.println("\n--- Test Suite 6: Customer Status Transitions ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            CustomerCreateRequest req = new CustomerCreateRequest("Usman Status Test", "+923006660001", "35201-6660001-1", "Lahore");
            AbizCustomer created = customerService.createCustomer(req);
            assertCondition("ACTIVE".equals(created.getStatus()), "Initial status must be ACTIVE");

            // Deactivate
            customerService.updateCustomerStatus(created.getId(), "INACTIVE");
            AbizCustomer deactivated = customerService.getCustomer(created.getId());
            assertCondition("INACTIVE".equals(deactivated.getStatus()), "Status must transition to INACTIVE");

            // Reactivate
            customerService.updateCustomerStatus(created.getId(), "ACTIVE");
            AbizCustomer reactivated = customerService.getCustomer(created.getId());
            assertCondition("ACTIVE".equals(reactivated.getStatus()), "Status must transition back to ACTIVE");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testRbacAndTenantBoundaries(
            CustomerService customerService, UUID shopA, UUID shopB,
            UUID ownerA, UUID staffA, UUID superAdmin) {
        System.out.println("\n--- Test Suite 7: RBAC & Strict Multi-Tenant Isolation ---");

        // 1. Create a customer in Shop B
        UUID ownerB = UUID.randomUUID();
        AbizCustomer shopBCustomer;
        AbizTenantContext.set(ownerB, AbizRole.SHOP_OWNER, shopB, "owner.b@test.com");
        try {
            CustomerCreateRequest bReq = new CustomerCreateRequest("Shop B Customer", "+923007770001", "35201-7770001-1", "Karachi");
            shopBCustomer = customerService.createCustomer(bReq);
        } finally {
            AbizTenantContext.clear();
        }

        // 2. Cross-Tenant Defense: Shop Owner A attempts to view Shop B customer
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            boolean crossTenantViewBlocked = false;
            try {
                customerService.getCustomer(shopBCustomer.getId());
            } catch (SecurityException e) {
                crossTenantViewBlocked = true;
            }
            assertCondition(crossTenantViewBlocked, "Shop Owner A MUST be strictly blocked from viewing Shop B customer");

            // Cross-Tenant Defense: Shop Owner A attempts to update Shop B customer
            boolean crossTenantUpdateBlocked = false;
            try {
                CustomerUpdateRequest updateReq = new CustomerUpdateRequest();
                updateReq.setName("Hacked Name");
                customerService.updateCustomer(shopBCustomer.getId(), updateReq);
            } catch (SecurityException e) {
                crossTenantUpdateBlocked = true;
            }
            assertCondition(crossTenantUpdateBlocked, "Shop Owner A MUST be strictly blocked from updating Shop B customer");

            // Cross-Tenant Defense: Shop Owner A attempts to create customer specifying shopId = Shop B
            boolean crossTenantCreateBlocked = false;
            try {
                CustomerCreateRequest hackCreate = new CustomerCreateRequest("Tamper", "+923008880001", "35201-8880001-1", "Karachi");
                hackCreate.setShopId(shopB); // Tampering attempt
                customerService.createCustomer(hackCreate);
            } catch (SecurityException e) {
                crossTenantCreateBlocked = true;
            }
            assertCondition(crossTenantCreateBlocked, "Shop Owner A supplying Shop B in request body must be rejected");

        } finally {
            AbizTenantContext.clear();
        }

        // 3. Shop Staff A operations
        AbizTenantContext.set(staffA, AbizRole.SHOP_STAFF, shopA, "staff.a@test.com");
        try {
            // Staff A creates customer in Shop A (permitted)
            CustomerCreateRequest staffCustReq = new CustomerCreateRequest("Staff Enrolled Customer", "+923009990001", "35201-9990001-1", "Lahore");
            AbizCustomer staffCreated = customerService.createCustomer(staffCustReq);
            assertCondition(staffCreated != null && shopA.equals(staffCreated.getShopId()),
                    "Shop Staff A must be permitted to create customers in Shop A");

            // Staff A blocked from Shop B customer
            boolean staffCrossTenantBlocked = false;
            try {
                customerService.getCustomer(shopBCustomer.getId());
            } catch (SecurityException e) {
                staffCrossTenantBlocked = true;
            }
            assertCondition(staffCrossTenantBlocked, "Shop Staff A MUST be blocked from viewing Shop B customer");

        } finally {
            AbizTenantContext.clear();
        }

        // 4. Super Admin access
        AbizTenantContext.set(superAdmin, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            AbizCustomer adminViewedA = customerService.getCustomer(shopBCustomer.getId());
            assertCondition(adminViewedA != null, "Super Admin must have cross-tenant access to inspect any customer");

            List<AbizCustomer> allCustomers = customerService.listCustomers(null, null, null, 0, 100);
            assertCondition(allCustomers.size() >= 2, "Super Admin can list customers across all shops");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testAuditTrailVerification(SqlSession session) {
        System.out.println("\n--- Test Suite 8: Audit Trail Generation for Customer Events ---");
        Connection conn = session.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT action, result, count(1) AS cnt FROM abiz_audit_logs " +
                "WHERE action IN ('CUSTOMER_CREATED', 'CUSTOMER_UPDATED', 'CUSTOMER_STATUS_CHANGED', 'CROSS_TENANT_CUSTOMER_ACCESS_BLOCKED') " +
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
            assertCondition(totalLogged > 0, "Customer operational events must be recorded in abiz_audit_logs");
        } catch (Exception e) {
            assertCondition(false, "Failed to query abiz_audit_logs: " + e.getMessage());
        }
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
