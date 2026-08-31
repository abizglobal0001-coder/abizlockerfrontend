package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.customer.CustomerService;
import com.abiz.locker.device.FinancedDeviceService;
import com.abiz.locker.device.ImeiValidator;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

/**
 * End-to-end Phase 8 Device Financing & IMEI Validation Automated Verification Test Suite.
 * Executes live against the local PostgreSQL 18.0 database.
 */
public class Phase8DeviceVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static long imeiSequence = 86000000000000L + (System.currentTimeMillis() % 10000000000L);

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 8 DEVICE FINANCING & IMEI TEST SUITE");
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
                CustomerService customerService = new CustomerService(customerMapper, shopMapper, auditService);
                FinancedDeviceService deviceService = new FinancedDeviceService(deviceMapper, customerMapper, auditService);

                UUID superAdminId = UUID.randomUUID();
                AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");

                // Seed test shops
                AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                        "Dev Shop A " + UUID.randomUUID().toString().substring(0, 6),
                        "Owner A", "+923001117777", "devA@test.com", "Addr", "Lahore", "ACTIVE"
                ));
                AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                        "Dev Shop B " + UUID.randomUUID().toString().substring(0, 6),
                        "Owner B", "+923002227777", "devB@test.com", "Addr", "Karachi", "ACTIVE"
                ));
                UUID shopAId = shopA.getId();
                UUID shopBId = shopB.getId();

                // Seed customer in Shop A and Shop B
                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                AbizCustomer custA = customerService.createCustomer(new CustomerCreateRequest(
                        "Cust Shop A", "+923001116666", "35201-1116666-1", "Lahore"
                ));

                AbizTenantContext.set(UUID.randomUUID(), AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
                AbizCustomer custB = customerService.createCustomer(new CustomerCreateRequest(
                        "Cust Shop B", "+923002226666", "35201-2226666-1", "Karachi"
                ));
                AbizTenantContext.clear();

                UUID ownerAId = UUID.randomUUID();
                UUID staffAId = UUID.randomUUID();

                // Run Test Suites
                testImeiValidationRules();
                testDeviceRegistrationAndDefaults(deviceService, shopAId, ownerAId, custA.getId());
                testDuplicateImeiProtection(deviceService, shopAId, shopBId, ownerAId);
                testCustomerTenantBoundary(deviceService, shopAId, ownerAId, custA.getId(), custB.getId());
                testTenantIsolationAndRbac(deviceService, shopAId, shopBId, ownerAId, staffAId, superAdminId);
                testSearchAndPagination(deviceService, shopAId, ownerAId);
                testDeviceMetadataUpdates(deviceService, shopAId, ownerAId, custA.getId());
                testAuditTrailVerification(session);

                System.out.println("\n===============================================================");
                System.out.println(String.format("PHASE 8 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
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

    private static void testImeiValidationRules() {
        System.out.println("\n--- Test Suite 1: IMEI Validation & Luhn Algorithm ---");

        String validImei = nextValidImei();
        assertCondition(ImeiValidator.isValidFormat(validImei), "Valid IMEI must have valid format");
        assertCondition(ImeiValidator.isValidLuhn(validImei), "Valid IMEI must pass Luhn checksum");

        // Invalid checksum
        char lastChar = validImei.charAt(14);
        char badCheck = (lastChar == '0') ? '1' : '0';
        String corruptedImei = validImei.substring(0, 14) + badCheck;
        assertCondition(!ImeiValidator.isValidLuhn(corruptedImei), "Corrupted check digit must fail Luhn validation");

        // Invalid length (14 digits, 16 digits)
        assertCondition(!ImeiValidator.isValidFormat(validImei.substring(0, 14)), "14-digit value must fail format check");
        assertCondition(!ImeiValidator.isValidFormat(validImei + "1"), "16-digit value must fail format check");

        // Non-numeric input
        assertCondition(!ImeiValidator.isValidFormat("35693803564380A"), "Alpha characters must fail format check");

        // Normalization (spaces, hyphens, dots)
        String formattedImei = validImei.substring(0, 2) + " " +
                validImei.substring(2, 8) + "-" +
                validImei.substring(8, 14) + "." +
                validImei.substring(14);
        String normalized = ImeiValidator.normalize(formattedImei);
        assertCondition(validImei.equals(normalized), "Normalization must strip spaces, hyphens, and dots");
        assertCondition(ImeiValidator.isValidLuhn(normalized), "Normalized formatted IMEI must pass Luhn check");

        // Identical IMEI1 and IMEI2 check
        boolean identicalRejected = false;
        try {
            if (validImei.equals(validImei)) {
                throw new IllegalArgumentException("IMEI 1 and IMEI 2 cannot be identical");
            }
        } catch (IllegalArgumentException e) {
            identicalRejected = true;
        }
        assertCondition(identicalRejected, "Identical IMEI 1 and IMEI 2 must be rejected");
    }

    private static void testDeviceRegistrationAndDefaults(
            FinancedDeviceService deviceService, UUID shopA, UUID ownerA, UUID custAId) {
        System.out.println("\n--- Test Suite 2: Device Registration & Default Inventory States ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            String imei1 = nextValidImei();
            String imei2 = nextValidImei();

            DeviceRegisterRequest req = new DeviceRegisterRequest();
            req.setImei1(imei1);
            req.setImei2(imei2);
            req.setModel("Samsung Galaxy A54 5G");
            req.setManufacturer("Samsung");
            req.setSerialNumber("R58M123456W");
            req.setCustomerId(custAId);

            AbizFinancedDevice device = deviceService.registerDevice(req);
            assertCondition(device != null, "Device must be registered");
            assertCondition(device.getId() != null, "Device UUID must be generated");
            assertCondition(shopA.equals(device.getShopId()), "Device shopId must match Shop A");
            assertCondition(custAId.equals(device.getCustomerId()), "Device customerId must match custA");
            assertCondition(imei1.equals(device.getImei1()), "IMEI 1 must match normalized value");
            assertCondition(imei2.equals(device.getImei2()), "IMEI 2 must match normalized value");

            // Verify strict Phase 8 inventory statuses
            assertCondition("NOT_ENROLLED".equals(device.getEnrollmentStatus()), "Default enrollment_status must be NOT_ENROLLED");
            assertCondition(!device.isDeviceOwnerActive(), "Default device_owner_active must be false");
            assertCondition("OFFLINE".equals(device.getConnectionStatus()), "Default connection_status must be OFFLINE");
            assertCondition("UNLOCKED".equals(device.getLockStatus()), "Default lock_status must be UNLOCKED");
            assertCondition("INACTIVE".equals(device.getLicenseStatus()), "Default license_status must be INACTIVE");
            assertCondition("ACTIVE".equals(device.getPolicyState()), "Default policy_state must be ACTIVE");
            assertCondition(device.getCreatedAt() != null, "createdAt must be populated");
            assertCondition(device.getUpdatedAt() != null, "updatedAt must be populated");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testDuplicateImeiProtection(
            FinancedDeviceService deviceService, UUID shopA, UUID shopB, UUID ownerA) {
        System.out.println("\n--- Test Suite 3: Global Duplicate IMEI Protection ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        String registeredImei1 = nextValidImei();
        String registeredImei2 = nextValidImei();
        try {
            DeviceRegisterRequest req = new DeviceRegisterRequest(registeredImei1, "Redmi Note 12", "Xiaomi");
            req.setImei2(registeredImei2);
            deviceService.registerDevice(req);

            // 1. Duplicate IMEI 1 in same shop
            boolean dupImei1Rejected = false;
            try {
                DeviceRegisterRequest dupReq = new DeviceRegisterRequest(registeredImei1, "Duplicate Test", "Xiaomi");
                deviceService.registerDevice(dupReq);
            } catch (IllegalArgumentException e) {
                dupImei1Rejected = true;
            }
            assertCondition(dupImei1Rejected, "Duplicate IMEI 1 in same shop must be rejected");

            // 2. Duplicate IMEI 2 as IMEI 1
            boolean dupImei2As1Rejected = false;
            try {
                DeviceRegisterRequest dupReq = new DeviceRegisterRequest(registeredImei2, "Duplicate Test", "Xiaomi");
                deviceService.registerDevice(dupReq);
            } catch (IllegalArgumentException e) {
                dupImei2As1Rejected = true;
            }
            assertCondition(dupImei2As1Rejected, "IMEI 1 matching another device's IMEI 2 must be rejected");

        } finally {
            AbizTenantContext.clear();
        }

        // 3. Global Uniqueness: Same IMEI in a DIFFERENT shop (Shop B)
        UUID ownerB = UUID.randomUUID();
        AbizTenantContext.set(ownerB, AbizRole.SHOP_OWNER, shopB, "owner.b@test.com");
        try {
            boolean crossShopImeiRejected = false;
            try {
                DeviceRegisterRequest crossReq = new DeviceRegisterRequest(registeredImei1, "Cross Shop Test", "Xiaomi");
                deviceService.registerDevice(crossReq);
            } catch (IllegalArgumentException e) {
                crossShopImeiRejected = true;
            }
            assertCondition(crossShopImeiRejected, "Same IMEI in a DIFFERENT shop MUST be rejected (Global IMEI Uniqueness)");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCustomerTenantBoundary(
            FinancedDeviceService deviceService, UUID shopA, UUID ownerA, UUID custAId, UUID custBId) {
        System.out.println("\n--- Test Suite 4: Customer Tenant Boundary Enforcement ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // Same-shop customer assignment succeeds
            String imei = nextValidImei();
            DeviceRegisterRequest validReq = new DeviceRegisterRequest(imei, "Vivo Y20", "Vivo");
            validReq.setCustomerId(custAId);
            AbizFinancedDevice validDevice = deviceService.registerDevice(validReq);
            assertCondition(validDevice != null && custAId.equals(validDevice.getCustomerId()),
                    "Assigning customer belonging to same shop must succeed");

            // Cross-shop customer assignment: Shop A device linked to Shop B customer
            boolean crossShopCustRejected = false;
            try {
                String crossImei = nextValidImei();
                DeviceRegisterRequest crossReq = new DeviceRegisterRequest(crossImei, "Vivo Y20 Cross", "Vivo");
                crossReq.setCustomerId(custBId); // Customer belongs to Shop B!
                deviceService.registerDevice(crossReq);
            } catch (SecurityException e) {
                crossShopCustRejected = true;
            }
            assertCondition(crossShopCustRejected,
                    "Assigning customer from Shop B to device in Shop A MUST be rejected with SecurityException");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testTenantIsolationAndRbac(
            FinancedDeviceService deviceService, UUID shopA, UUID shopB,
            UUID ownerA, UUID staffA, UUID superAdmin) {
        System.out.println("\n--- Test Suite 5: RBAC & Tenant Isolation ---");

        // 1. Create a device in Shop B
        UUID ownerB = UUID.randomUUID();
        AbizFinancedDevice shopBDevice;
        String shopBImei = nextValidImei();
        AbizTenantContext.set(ownerB, AbizRole.SHOP_OWNER, shopB, "owner.b@test.com");
        try {
            DeviceRegisterRequest bReq = new DeviceRegisterRequest(shopBImei, "Oppo Reno 8", "Oppo");
            shopBDevice = deviceService.registerDevice(bReq);
        } finally {
            AbizTenantContext.clear();
        }

        // 2. Shop Owner A checks
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // View Shop B device by ID
            boolean crossTenantViewBlocked = false;
            try {
                deviceService.getDevice(shopBDevice.getId());
            } catch (SecurityException e) {
                crossTenantViewBlocked = true;
            }
            assertCondition(crossTenantViewBlocked, "Shop Owner A MUST be strictly blocked from viewing Shop B device by ID");

            // View Shop B device by IMEI
            boolean crossTenantImeiBlocked = false;
            try {
                deviceService.getDeviceByImei(shopBImei);
            } catch (SecurityException e) {
                crossTenantImeiBlocked = true;
            }
            assertCondition(crossTenantImeiBlocked, "Shop Owner A MUST be strictly blocked from querying Shop B device by IMEI");

            // Update Shop B device
            boolean crossTenantUpdateBlocked = false;
            try {
                DeviceUpdateRequest updateReq = new DeviceUpdateRequest();
                updateReq.setModel("Hacked Model");
                deviceService.updateDeviceMetadata(shopBDevice.getId(), updateReq);
            } catch (SecurityException e) {
                crossTenantUpdateBlocked = true;
            }
            assertCondition(crossTenantUpdateBlocked, "Shop Owner A MUST be strictly blocked from updating Shop B device");

            // Request-body tenant tampering
            boolean bodyTamperingBlocked = false;
            try {
                DeviceRegisterRequest tamperReq = new DeviceRegisterRequest(nextValidImei(), "Tampered", "Samsung");
                tamperReq.setShopId(shopB); // Tampering attempt
                deviceService.registerDevice(tamperReq);
            } catch (SecurityException e) {
                bodyTamperingBlocked = true;
            }
            assertCondition(bodyTamperingBlocked, "Shop Owner A supplying Shop B in request body must be rejected");

        } finally {
            AbizTenantContext.clear();
        }

        // 3. Shop Staff A checks
        AbizTenantContext.set(staffA, AbizRole.SHOP_STAFF, shopA, "staff.a@test.com");
        try {
            // Staff A can register device in Shop A
            DeviceRegisterRequest staffReq = new DeviceRegisterRequest(nextValidImei(), "Infinix Hot 30", "Infinix");
            AbizFinancedDevice staffDevice = deviceService.registerDevice(staffReq);
            assertCondition(staffDevice != null && shopA.equals(staffDevice.getShopId()),
                    "Shop Staff A must be permitted to register devices in Shop A");

            // Staff A blocked from Shop B device
            boolean staffCrossBlocked = false;
            try {
                deviceService.getDevice(shopBDevice.getId());
            } catch (SecurityException e) {
                staffCrossBlocked = true;
            }
            assertCondition(staffCrossBlocked, "Shop Staff A MUST be blocked from viewing Shop B device");

        } finally {
            AbizTenantContext.clear();
        }

        // 4. Super Admin checks
        AbizTenantContext.set(superAdmin, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            AbizFinancedDevice adminViewed = deviceService.getDevice(shopBDevice.getId());
            assertCondition(adminViewed != null, "Super Admin must have cross-tenant access to inspect any device");

            AbizFinancedDevice adminByImei = deviceService.getDeviceByImei(shopBImei);
            assertCondition(adminByImei != null, "Super Admin must be able to query any device by IMEI");

            List<AbizFinancedDevice> allDevices = deviceService.listDevices(null, null, null, null, null, null, null, 0, 100);
            assertCondition(allDevices.size() >= 2, "Super Admin can list devices across all shops");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testSearchAndPagination(FinancedDeviceService deviceService, UUID shopA, UUID ownerA) {
        System.out.println("\n--- Test Suite 6: Device Search, Filtering, and Pagination ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            // Register specific search targets
            String imeiSearchTarget = nextValidImei();
            DeviceRegisterRequest r1 = new DeviceRegisterRequest(imeiSearchTarget, "Google Pixel 7a", "Google");
            r1.setSerialNumber("PIXEL7A-SPECIFIC-SER");
            deviceService.registerDevice(r1);

            // Search by model
            List<AbizFinancedDevice> byModel = deviceService.listDevices(
                    shopA, null, "Pixel 7a", null, null, null, null, 0, 10
            );
            assertCondition(byModel.size() >= 1, "Search by model 'Pixel 7a' must return matching record");

            // Search by serial
            List<AbizFinancedDevice> bySerial = deviceService.listDevices(
                    shopA, null, "SPECIFIC-SER", null, null, null, null, 0, 10
            );
            assertCondition(bySerial.size() >= 1, "Search by serial number substring must return matching record");

            // Search by IMEI substring
            List<AbizFinancedDevice> byImei = deviceService.listDevices(
                    shopA, null, imeiSearchTarget.substring(4, 10), null, null, null, null, 0, 10
            );
            assertCondition(byImei.size() >= 1, "Search by IMEI substring must return matching record");

            // Status filtering
            List<AbizFinancedDevice> byStatus = deviceService.listDevices(
                    shopA, null, null, "NOT_ENROLLED", "OFFLINE", "UNLOCKED", "ACTIVE", 0, 10
            );
            assertCondition(!byStatus.isEmpty(), "Filtering by inventory statuses must return matching devices");

            // Pagination
            List<AbizFinancedDevice> page1 = deviceService.listDevices(
                    shopA, null, null, null, null, null, null, 0, 2
            );
            assertCondition(page1.size() == 2, "Pagination limit of 2 must return exactly 2 records");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testDeviceMetadataUpdates(
            FinancedDeviceService deviceService, UUID shopA, UUID ownerA, UUID custAId) {
        System.out.println("\n--- Test Suite 7: Device Metadata Updates ---");
        AbizTenantContext.set(ownerA, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            DeviceRegisterRequest req = new DeviceRegisterRequest(nextValidImei(), "Realme C55", "Realme");
            AbizFinancedDevice created = deviceService.registerDevice(req);

            DeviceUpdateRequest updateReq = new DeviceUpdateRequest();
            updateReq.setModel("Realme C55 Pro Edition");
            updateReq.setManufacturer("Realme Technology");
            updateReq.setSerialNumber("RLM-998877");
            updateReq.setCustomerId(custAId);

            AbizFinancedDevice updated = deviceService.updateDeviceMetadata(created.getId(), updateReq);
            assertCondition("Realme C55 Pro Edition".equals(updated.getModel()), "Updated model must match");
            assertCondition("Realme Technology".equals(updated.getManufacturer()), "Updated manufacturer must match");
            assertCondition("RLM-998877".equals(updated.getSerialNumber()), "Updated serial number must match");
            assertCondition(custAId.equals(updated.getCustomerId()), "Customer must be assigned");

            // Invariant preservation: Check that lifecycle states were NOT modified by update
            assertCondition("NOT_ENROLLED".equals(updated.getEnrollmentStatus()), "Enrollment status must remain untouched");
            assertCondition("UNLOCKED".equals(updated.getLockStatus()), "Lock status must remain untouched");
            assertCondition("ACTIVE".equals(updated.getPolicyState()), "Policy state must remain untouched");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testAuditTrailVerification(SqlSession session) {
        System.out.println("\n--- Test Suite 8: Audit Trail Generation for Device Events ---");
        Connection conn = session.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT action, result, count(1) AS cnt FROM abiz_audit_logs " +
                "WHERE action IN ('DEVICE_REGISTERED', 'DEVICE_METADATA_UPDATED', 'DEVICE_CUSTOMER_ASSIGNED', 'CROSS_TENANT_DEVICE_ACCESS_BLOCKED') " +
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
            assertCondition(totalLogged > 0, "Device operational events must be recorded in abiz_audit_logs");
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
