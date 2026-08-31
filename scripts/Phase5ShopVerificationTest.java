package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.domain.AbizShop;
import com.abiz.locker.domain.ShopCreateRequest;
import com.abiz.locker.domain.ShopStatusUpdateRequest;
import com.abiz.locker.domain.ShopUpdateRequest;
import com.abiz.locker.mapper.AuditMapper;
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
 * End-to-end Phase 5 Shop Management & Multi-Tenancy Automated Verification Test Suite.
 * Executes live against the local PostgreSQL 18.0 database.
 */
public class Phase5ShopVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 5 SHOP MANAGEMENT & MULTI-TENANCY TEST SUITE");
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

            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = factory.openSession(true)) {
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                Field auditMapperField = AbizAuditService.class.getDeclaredField("auditMapper");
                auditMapperField.setAccessible(true);
                auditMapperField.set(auditService, auditMapper);

                ShopService shopService = new ShopService(shopMapper, auditService);

                UUID superAdminId = UUID.randomUUID();
                AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
                AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                        "Shop A " + UUID.randomUUID().toString().substring(0, 6),
                        "Owner A", "+923001111111", "shopA@test.com", "Addr A", "Lahore", "ACTIVE"
                ));
                AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                        "Shop B " + UUID.randomUUID().toString().substring(0, 6),
                        "Owner B", "+923002222222", "shopB@test.com", "Addr B", "Karachi", "ACTIVE"
                ));
                UUID shopAId = shopA.getId();
                UUID shopBId = shopB.getId();
                AbizTenantContext.clear();

                UUID shopOwnerAId = UUID.randomUUID();
                UUID shopStaffAId = UUID.randomUUID();

                // Run Test Suites
                testSuperAdminShopCreation(shopService, superAdminId);
                testInputValidationAndDuplicateRejection(shopService, superAdminId);
                testSuperAdminRetrievalAndListing(shopService, superAdminId);
                testSuperAdminProfileUpdate(shopService, superAdminId);
                testLifecycleStateTransitions(shopService, superAdminId);
                testShopOwnerAccessBoundaries(shopService, shopAId, shopBId, shopOwnerAId);
                testShopStaffAccessRejection(shopService, shopAId, shopStaffAId);
                testCrossTenantManualParameterDefense(shopService, shopAId, shopBId, shopOwnerAId);
                testAuditLoggingVerification(session);

                System.out.println("\n===============================================================");
                System.out.println(String.format("PHASE 5 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
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

    private static void testSuperAdminShopCreation(ShopService service, UUID adminId) {
        System.out.println("\n--- Test Suite 1: Super Admin Shop Creation ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
            String shopName = "Galaxy Mobiles " + uniqueSuffix;
            ShopCreateRequest req = new ShopCreateRequest(
                    shopName,
                    "Farhan Khan",
                    "+923005551234",
                    "galaxy." + uniqueSuffix + "@shop.com",
                    "Suite 101, Hafeez Center",
                    "Lahore",
                    "ACTIVE"
            );

            AbizShop created = service.createShop(req);
            assertCondition(created != null, "Shop creation must return created domain model");
            assertCondition(created.getId() != null, "Shop ID must be generated by database");
            assertCondition(shopName.equals(created.getName()), "Shop name must match request");
            assertCondition("ACTIVE".equals(created.getStatus()), "Shop status must default to ACTIVE");
            assertCondition("Lahore".equals(created.getCity()), "Shop city must match");
            assertCondition(created.getCreatedAt() != null, "Created timestamp must be populated");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testInputValidationAndDuplicateRejection(ShopService service, UUID adminId) {
        System.out.println("\n--- Test Suite 2: Input Validation & Duplicate Rejection ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            String duplicateName = "Duplicate Test Shop " + UUID.randomUUID().toString().substring(0, 6);
            ShopCreateRequest req1 = new ShopCreateRequest(
                    duplicateName, "Owner 1", "+923001112233", "dup1@test.com", "Address", "Karachi", "ACTIVE"
            );
            service.createShop(req1);

            // Duplicate creation attempt
            boolean duplicateRejected = false;
            try {
                service.createShop(req1);
            } catch (IllegalArgumentException e) {
                duplicateRejected = true;
            }
            assertCondition(duplicateRejected, "Duplicate shop name must be rejected with IllegalArgumentException");

            // Empty name rejection
            boolean emptyNameRejected = false;
            try {
                service.createShop(new ShopCreateRequest("", "Owner", "+923001112233", null, null, null, "ACTIVE"));
            } catch (IllegalArgumentException e) {
                emptyNameRejected = true;
            }
            assertCondition(emptyNameRejected, "Empty shop name must be rejected");

            // Short phone rejection
            boolean shortPhoneRejected = false;
            try {
                service.createShop(new ShopCreateRequest("Valid Name", "Owner", "123", null, null, null, "ACTIVE"));
            } catch (IllegalArgumentException e) {
                shortPhoneRejected = true;
            }
            assertCondition(shortPhoneRejected, "Short phone number (<7 chars) must be rejected");

            // Invalid email rejection
            boolean invalidEmailRejected = false;
            try {
                service.createShop(new ShopCreateRequest("Valid Name 2", "Owner", "+923001112233", "invalid-email", null, null, "ACTIVE"));
            } catch (IllegalArgumentException e) {
                invalidEmailRejected = true;
            }
            assertCondition(invalidEmailRejected, "Invalid email format must be rejected");

            // Invalid initial status rejection
            boolean invalidStatusRejected = false;
            try {
                service.createShop(new ShopCreateRequest("Valid Name 3", "Owner", "+923001112233", "ok@test.com", null, null, "SUSPENDED"));
            } catch (IllegalArgumentException e) {
                invalidStatusRejected = true;
            }
            assertCondition(invalidStatusRejected, "Initial status other than ACTIVE or PENDING must be rejected");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testSuperAdminRetrievalAndListing(ShopService service, UUID adminId) {
        System.out.println("\n--- Test Suite 3: Super Admin Retrieval & Listing ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            List<AbizShop> allShops = service.listShops(null, 0, 50);
            assertCondition(allShops != null && !allShops.isEmpty(), "Super Admin listShops must return non-empty list");

            List<AbizShop> activeShops = service.listShops("ACTIVE", 0, 50);
            assertCondition(activeShops != null && !activeShops.isEmpty(), "Super Admin listShops with 'ACTIVE' filter must succeed");
            for (AbizShop s : activeShops) {
                if (!"ACTIVE".equals(s.getStatus())) {
                    assertCondition(false, "Filtered list contained non-ACTIVE shop: " + s.getStatus());
                    return;
                }
            }
            assertCondition(true, "All shops in active list have status = ACTIVE");

            AbizShop first = allShops.get(0);
            AbizShop fetched = service.getShopById(first.getId());
            assertCondition(fetched != null && fetched.getId().equals(first.getId()), "Super Admin getShopById must fetch correct shop");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testSuperAdminProfileUpdate(ShopService service, UUID adminId) {
        System.out.println("\n--- Test Suite 4: Super Admin Profile Update ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            String shopName = "Updatable Shop " + UUID.randomUUID().toString().substring(0, 6);
            AbizShop created = service.createShop(new ShopCreateRequest(
                    shopName, "Initial Owner", "+923001110000", "initial@shop.com", "Initial Addr", "Islamabad", "ACTIVE"
            ));

            ShopUpdateRequest updateReq = new ShopUpdateRequest(
                    shopName + " Renamed",
                    "Updated Owner Name",
                    "+923009998877",
                    "updated@shop.com",
                    "New Commercial Market",
                    "Rawalpindi"
            );

            AbizShop updated = service.updateShop(created.getId(), updateReq);
            assertCondition((shopName + " Renamed").equals(updated.getName()), "Updated name must match");
            assertCondition("Updated Owner Name".equals(updated.getOwnerName()), "Updated owner name must match");
            assertCondition("+923009998877".equals(updated.getPhone()), "Updated phone must match");
            assertCondition("Rawalpindi".equals(updated.getCity()), "Updated city must match");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testLifecycleStateTransitions(ShopService service, UUID adminId) {
        System.out.println("\n--- Test Suite 5: Lifecycle State Machine Transitions ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            // Case 1: ACTIVE -> SUSPENDED -> ACTIVE
            AbizShop activeShop = service.createShop(new ShopCreateRequest(
                    "Lifecycle Shop " + UUID.randomUUID().toString().substring(0, 6),
                    "Owner", "+923001234567", null, null, "Lahore", "ACTIVE"
            ));

            AbizShop suspended = service.updateShopStatus(
                    activeShop.getId(),
                    new ShopStatusUpdateRequest("SUSPENDED", "Overdue license fees")
            );
            assertCondition("SUSPENDED".equals(suspended.getStatus()), "ACTIVE -> SUSPENDED transition must succeed");

            AbizShop reinstated = service.updateShopStatus(
                    activeShop.getId(),
                    new ShopStatusUpdateRequest("ACTIVE", "Account settled")
            );
            assertCondition("ACTIVE".equals(reinstated.getStatus()), "SUSPENDED -> ACTIVE transition must succeed");

            // Case 2: PENDING -> ACTIVE (Approval)
            AbizShop pendingShop = service.createShop(new ShopCreateRequest(
                    "Pending Shop " + UUID.randomUUID().toString().substring(0, 6),
                    "Owner", "+923001234568", null, null, "Karachi", "PENDING"
            ));
            assertCondition("PENDING".equals(pendingShop.getStatus()), "Shop created with initial status PENDING");

            AbizShop approved = service.updateShopStatus(
                    pendingShop.getId(),
                    new ShopStatusUpdateRequest("ACTIVE", "Shop KYC verified")
            );
            assertCondition("ACTIVE".equals(approved.getStatus()), "PENDING -> ACTIVE approval must succeed");

            // Case 3: PENDING -> SUSPENDED (Rejection)
            AbizShop pendingShop2 = service.createShop(new ShopCreateRequest(
                    "Pending Shop 2 " + UUID.randomUUID().toString().substring(0, 6),
                    "Owner", "+923001234569", null, null, "Multan", "PENDING"
            ));
            AbizShop rejected = service.updateShopStatus(
                    pendingShop2.getId(),
                    new ShopStatusUpdateRequest("SUSPENDED", "KYC documents rejected")
            );
            assertCondition("SUSPENDED".equals(rejected.getStatus()), "PENDING -> SUSPENDED rejection must succeed");

            // Case 4: Disallowed transition: ACTIVE -> PENDING (Backward transition)
            boolean backwardRejected = false;
            try {
                service.updateShopStatus(approved.getId(), new ShopStatusUpdateRequest("PENDING", "Reset"));
            } catch (IllegalStateException e) {
                backwardRejected = true;
            }
            assertCondition(backwardRejected, "ACTIVE -> PENDING backward transition must be rejected");

            // Case 5: Disallowed transition: SUSPENDED -> PENDING
            boolean suspendedToPendingRejected = false;
            try {
                service.updateShopStatus(rejected.getId(), new ShopStatusUpdateRequest("PENDING", "Reset"));
            } catch (IllegalStateException e) {
                suspendedToPendingRejected = true;
            }
            assertCondition(suspendedToPendingRejected, "SUSPENDED -> PENDING backward transition must be rejected");

            // Case 6: No-op transition: ACTIVE -> ACTIVE
            boolean noOpRejected = false;
            try {
                service.updateShopStatus(approved.getId(), new ShopStatusUpdateRequest("ACTIVE", "No change"));
            } catch (IllegalStateException e) {
                noOpRejected = true;
            }
            assertCondition(noOpRejected, "No-op status transition (ACTIVE -> ACTIVE) must be rejected");

            // Case 7: Invalid status string
            boolean invalidStatusRejected = false;
            try {
                service.updateShopStatus(approved.getId(), new ShopStatusUpdateRequest("TERMINATED", "Invalid"));
            } catch (IllegalArgumentException e) {
                invalidStatusRejected = true;
            }
            assertCondition(invalidStatusRejected, "Unknown status string must be rejected with IllegalArgumentException");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testShopOwnerAccessBoundaries(ShopService service, UUID shopA, UUID shopB, UUID ownerAId) {
        System.out.println("\n--- Test Suite 6: Shop Owner Access Boundaries & Tenant Isolation ---");
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopA, "owner.a@citymobiles.com");
        try {
            // Can view own shop
            AbizShop myShop = service.getShopById(shopA);
            assertCondition(myShop != null && shopA.equals(myShop.getId()), "Shop Owner must be able to view their own shop");

            // STRICT MULTI-TENANT ISOLATION: Cannot view Shop B!
            boolean crossTenantBlocked = false;
            try {
                service.getShopById(shopB);
            } catch (SecurityException e) {
                crossTenantBlocked = true;
            }
            assertCondition(crossTenantBlocked, "Shop Owner A MUST be strictly blocked from viewing Shop B (Tenant Isolation)");

            // Cannot create shops
            boolean createBlocked = false;
            try {
                service.createShop(new ShopCreateRequest("Hacker Shop", "Owner", "+923000000000", null, null, null, "ACTIVE"));
            } catch (SecurityException e) {
                createBlocked = true;
            }
            assertCondition(createBlocked, "Shop Owner MUST be blocked from creating shops (Privilege Escalation Defense)");

            // Cannot update shop administration
            boolean updateBlocked = false;
            try {
                service.updateShop(shopA, new ShopUpdateRequest("Hacked Name", null, null, null, null, null));
            } catch (SecurityException e) {
                updateBlocked = true;
            }
            assertCondition(updateBlocked, "Shop Owner MUST be blocked from updating shop administrative details");

            // Cannot modify shop status
            boolean statusBlocked = false;
            try {
                service.updateShopStatus(shopA, new ShopStatusUpdateRequest("SUSPENDED", "Self suspend"));
            } catch (SecurityException e) {
                statusBlocked = true;
            }
            assertCondition(statusBlocked, "Shop Owner MUST be blocked from altering shop status");

            // Cannot list platform shops
            boolean listBlocked = false;
            try {
                service.listShops(null, 0, 20);
            } catch (SecurityException e) {
                listBlocked = true;
            }
            assertCondition(listBlocked, "Shop Owner MUST be blocked from listing platform-wide shops");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testShopStaffAccessRejection(ShopService service, UUID shopA, UUID staffAId) {
        System.out.println("\n--- Test Suite 7: Shop Staff Access Rejection ---");
        AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopA, "staff.a@citymobiles.com");
        try {
            // Cannot view shop admin
            boolean viewBlocked = false;
            try {
                service.getShopById(shopA);
            } catch (SecurityException e) {
                viewBlocked = true;
            }
            assertCondition(viewBlocked, "Shop Staff MUST be blocked from shop management view");

            // Cannot create shop
            boolean createBlocked = false;
            try {
                service.createShop(new ShopCreateRequest("Staff Shop", "Owner", "+923000000000", null, null, null, "ACTIVE"));
            } catch (SecurityException e) {
                createBlocked = true;
            }
            assertCondition(createBlocked, "Shop Staff MUST be blocked from creating shops");

            // Cannot update shop
            boolean updateBlocked = false;
            try {
                service.updateShop(shopA, new ShopUpdateRequest("Staff Update", null, null, null, null, null));
            } catch (SecurityException e) {
                updateBlocked = true;
            }
            assertCondition(updateBlocked, "Shop Staff MUST be blocked from updating shops");

            // Cannot change status
            boolean statusBlocked = false;
            try {
                service.updateShopStatus(shopA, new ShopStatusUpdateRequest("ACTIVE", "Staff change"));
            } catch (SecurityException e) {
                statusBlocked = true;
            }
            assertCondition(statusBlocked, "Shop Staff MUST be blocked from updating shop status");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCrossTenantManualParameterDefense(ShopService service, UUID shopA, UUID shopB, UUID ownerAId) {
        System.out.println("\n--- Test Suite 8: Server-side Tenant Enforcement (Ignoring Request shopId) ---");
        // An attacker sets up an authenticated session as Owner of Shop A,
        // but manually passes shopB in an API URL: /rest/abiz/shops/{shopB}
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopA, "owner.a@citymobiles.com");
        try {
            boolean attackBlocked = false;
            try {
                // Service method receives targetShopId from path param, but enforces context.getShopId()
                service.getShopById(shopB);
            } catch (SecurityException e) {
                attackBlocked = true;
            }
            assertCondition(attackBlocked, "Client-supplied shopId parameter was rejected because principal is bound to Shop A");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testAuditLoggingVerification(SqlSession session) {
        System.out.println("\n--- Test Suite 9: Audit Trail Generation for Shop Events ---");
        Connection conn = session.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT action, result, count(1) AS cnt FROM abiz_audit_logs " +
                "WHERE action IN ('SHOP_CREATED', 'SHOP_UPDATED', 'SHOP_SUSPENDED', 'SHOP_ACTIVATED', 'CROSS_TENANT_ACCESS_BLOCKED') " +
                "GROUP BY action, result")) {
            ResultSet rs = ps.executeQuery();
            int totalLoggedEvents = 0;
            while (rs.next()) {
                String action = rs.getString("action");
                String result = rs.getString("result");
                int count = rs.getInt("cnt");
                totalLoggedEvents += count;
                System.out.println(String.format("  [AUDIT LOGGED] action=%s, result=%s, count=%d", action, result, count));
            }
            assertCondition(totalLoggedEvents > 0, "Shop operational events must be recorded in abiz_audit_logs");
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
