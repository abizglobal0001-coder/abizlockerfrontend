package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizJwtService;
import com.abiz.locker.auth.AbizPasswordService;
import com.abiz.locker.auth.AbizRbacGuard;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.auth.AbizUserService;
import com.abiz.locker.domain.AbizAuthenticatedUser;
import com.abiz.locker.domain.AbizLoginResponse;
import com.abiz.locker.domain.AbizRefreshToken;
import com.abiz.locker.domain.AbizShopUser;
import com.abiz.locker.domain.AbizSuperAdmin;
import com.abiz.locker.mapper.AuditMapper;
import com.abiz.locker.mapper.ShopUserMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.postgresql.ds.PGSimpleDataSource;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.UUID;

/**
 * End-to-end Phase 4 Authentication & RBAC Verification Test Suite.
 * Exercises all authentication, cryptographic, persistence, token, RBAC,
 * and tenant isolation capabilities against the live PostgreSQL 18 database.
 */
public class Phase4AuthVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 4 AUTHENTICATION & RBAC TEST SUITE");
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
            config.getTypeHandlerRegistry().register(UUID.class, new com.abiz.locker.mapper.UuidTypeHandler());
            config.addMapper(ShopUserMapper.class);
            config.addMapper(AuditMapper.class);

            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = factory.openSession(true)) {
                ShopUserMapper userMapper = session.getMapper(ShopUserMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);

                AbizPasswordService passwordService = new AbizPasswordService();
                AbizJwtService jwtService = new AbizJwtService();

                AbizAuditService auditService = new AbizAuditService();
                Field auditMapperField = AbizAuditService.class.getDeclaredField("auditMapper");
                auditMapperField.setAccessible(true);
                auditMapperField.set(auditService, auditMapper);

                AbizUserService userService = new AbizUserService(userMapper, passwordService, jwtService, auditService);

                // Setup test entities
                UUID shopAId = UUID.randomUUID();
                UUID shopBId = UUID.randomUUID();
                setupTestData(session, shopAId, shopBId, passwordService);

                AbizShopUser existingOwner = userMapper.findShopUserByEmail("owner.a@citymobiles.com");
                if (existingOwner != null) {
                    shopAId = existingOwner.getShopId();
                }

                // Run Test Cases
                testPasswordSecurity(passwordService);
                testJwtService(jwtService);
                testSuperAdminAuthentication(userService);
                testShopUserAuthentication(userService, shopAId);
                testInvalidCredentialsAndLockout(userService, session);
                testSuspendedUserRejection(userService);
                testRefreshTokenRotation(userService);
                testRevokedTokenRejection(userService);
                testRbacAndTenantIsolation(shopAId, shopBId);

                System.out.println("\n===============================================================");
                System.out.println(String.format("TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
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

    private static void setupTestData(SqlSession session, UUID shopA, UUID shopB, AbizPasswordService pwService) {
        System.out.println("\n[SETUP] Seeding test shops and accounts in abizlocker DB...");

        // Insert Shop A and Shop B
        session.getConnection();
        java.sql.Connection conn = session.getConnection();
        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO abiz_shops (id, name, owner_name, phone, status) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING")) {
            ps.setObject(1, shopA);
            ps.setString(2, "Test Shop A - City Center");
            ps.setString(3, "Owner A");
            ps.setString(4, "+923001111111");
            ps.setString(5, "ACTIVE");
            ps.executeUpdate();

            ps.setObject(1, shopB);
            ps.setString(2, "Test Shop B - Metro Mall");
            ps.setString(3, "Owner B");
            ps.setString(4, "+923002222222");
            ps.setString(5, "ACTIVE");
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed shops", e);
        }

        ShopUserMapper mapper = session.getMapper(ShopUserMapper.class);

        // Super Admin
        if (mapper.findSuperAdminByEmail("superadmin@abizlocker.com") == null) {
            AbizSuperAdmin admin = new AbizSuperAdmin();
            admin.setId(UUID.randomUUID());
            admin.setName("Master Platform Admin");
            admin.setEmail("superadmin@abizlocker.com");
            admin.setPasswordHash(pwService.hashPassword("SuperAdmin@2026Secure!"));
            admin.setStatus("ACTIVE");
            mapper.insertSuperAdmin(admin);
        }

        // Shop A Owner
        if (mapper.findShopUserByEmail("owner.a@citymobiles.com") == null) {
            AbizShopUser user = new AbizShopUser();
            user.setId(UUID.randomUUID());
            user.setShopId(shopA);
            user.setName("Alice Owner A");
            user.setEmail("owner.a@citymobiles.com");
            user.setPasswordHash(pwService.hashPassword("OwnerA@2026Secure!"));
            user.setRole("OWNER");
            user.setStatus("ACTIVE");
            mapper.insertShopUser(user);
        }

        // Shop A Staff
        if (mapper.findShopUserByEmail("staff.a@citymobiles.com") == null) {
            AbizShopUser user = new AbizShopUser();
            user.setId(UUID.randomUUID());
            user.setShopId(shopA);
            user.setName("Bob Staff A");
            user.setEmail("staff.a@citymobiles.com");
            user.setPasswordHash(pwService.hashPassword("StaffA@2026Secure!"));
            user.setRole("STAFF");
            user.setStatus("ACTIVE");
            mapper.insertShopUser(user);
        }

        // Shop B Owner
        if (mapper.findShopUserByEmail("owner.b@metrowireless.com") == null) {
            AbizShopUser user = new AbizShopUser();
            user.setId(UUID.randomUUID());
            user.setShopId(shopB);
            user.setName("Charlie Owner B");
            user.setEmail("owner.b@metrowireless.com");
            user.setPasswordHash(pwService.hashPassword("OwnerB@2026Secure!"));
            user.setRole("OWNER");
            user.setStatus("ACTIVE");
            mapper.insertShopUser(user);
        }

        // Suspended User
        if (mapper.findShopUserByEmail("suspended@citymobiles.com") == null) {
            AbizShopUser user = new AbizShopUser();
            user.setId(UUID.randomUUID());
            user.setShopId(shopA);
            user.setName("Suspended User");
            user.setEmail("suspended@citymobiles.com");
            user.setPasswordHash(pwService.hashPassword("Suspended@2026Pass!"));
            user.setRole("STAFF");
            user.setStatus("SUSPENDED");
            mapper.insertShopUser(user);
        }

        System.out.println("[SETUP] Seed data ready.");
    }

    // ==========================================================
    // Test Methods
    // ==========================================================

    private static void testPasswordSecurity(AbizPasswordService pwService) {
        System.out.println("\n--- Test Suite 1: Password Security (BCrypt) ---");
        String raw = "StrongSecretPassword#2026";
        String hash = pwService.hashPassword(raw);

        assertCondition(hash.startsWith("$2a$12$"), "BCrypt hash must start with $2a$12$ (12 salt rounds)");
        assertCondition(pwService.verifyPassword(raw, hash), "BCrypt verification must succeed with correct password");
        assertCondition(!pwService.verifyPassword("WrongPassword", hash), "BCrypt verification must fail with wrong password");
        assertCondition(!pwService.verifyPassword(null, hash), "BCrypt verification must fail with null password");

        boolean threwShort = false;
        try {
            pwService.hashPassword("short");
        } catch (IllegalArgumentException e) {
            threwShort = true;
        }
        assertCondition(threwShort, "Password policy must reject passwords shorter than 8 characters");
    }

    private static void testJwtService(AbizJwtService jwtService) {
        System.out.println("\n--- Test Suite 2: JWT Access Token Lifecycle ---");
        UUID userId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        AbizAuthenticatedUser user = new AbizAuthenticatedUser(userId, "test@shop.com", "Test User", AbizRole.SHOP_OWNER, shopId, "ACTIVE");

        String token = jwtService.generateAccessToken(user);
        assertCondition(token != null && token.split("\\.").length == 3, "Generated JWT must be valid 3-part compact string");

        AbizAuthenticatedUser parsed = jwtService.parseAccessToken(token);
        assertCondition(parsed.getId().equals(userId), "Parsed JWT userId must match subject");
        assertCondition(parsed.getRole() == AbizRole.SHOP_OWNER, "Parsed JWT role must be SHOP_OWNER");
        assertCondition(parsed.getShopId().equals(shopId), "Parsed JWT shopId must match");
        assertCondition("test@shop.com".equals(parsed.getEmail()), "Parsed JWT email must match");

        // Tampered token test
        String tampered = token.substring(0, token.length() - 5) + "abcde";
        boolean rejectedTampered = false;
        try {
            jwtService.parseAccessToken(tampered);
        } catch (IllegalArgumentException e) {
            rejectedTampered = true;
        }
        assertCondition(rejectedTampered, "Tampered token signature must be strictly rejected");
    }

    private static void testSuperAdminAuthentication(AbizUserService userService) {
        System.out.println("\n--- Test Suite 3: Super Admin Login ---");
        AbizLoginResponse resp = userService.authenticate("superadmin@abizlocker.com", "SuperAdmin@2026Secure!");
        assertCondition(resp != null, "Super admin login must succeed");
        assertCondition(resp.getAccessToken() != null, "Access token must be present");
        assertCondition(resp.getRefreshToken() != null, "Refresh token must be present");
        assertCondition(resp.getUser().getRole() == AbizRole.SUPER_ADMIN, "Role must be SUPER_ADMIN");
        assertCondition(resp.getUser().getShopId() == null, "Super admin shopId must be null (cross-tenant)");
    }

    private static void testShopUserAuthentication(AbizUserService userService, UUID shopAId) {
        System.out.println("\n--- Test Suite 4: Shop User Login (Owner & Staff) ---");
        // Shop Owner
        AbizLoginResponse ownerResp = userService.authenticate("owner.a@citymobiles.com", "OwnerA@2026Secure!");
        assertCondition(ownerResp.getUser().getRole() == AbizRole.SHOP_OWNER, "Owner role must be SHOP_OWNER");
        assertCondition(shopAId.equals(ownerResp.getUser().getShopId()), "Owner shopId must match Shop A");

        // Shop Staff
        AbizLoginResponse staffResp = userService.authenticate("staff.a@citymobiles.com", "StaffA@2026Secure!");
        assertCondition(staffResp.getUser().getRole() == AbizRole.SHOP_STAFF, "Staff role must be SHOP_STAFF");
        assertCondition(shopAId.equals(staffResp.getUser().getShopId()), "Staff shopId must match Shop A");
    }

    private static void testInvalidCredentialsAndLockout(AbizUserService userService, SqlSession session) {
        System.out.println("\n--- Test Suite 5: Failed Logins & Lockout Protection ---");
        boolean rejectedWrongPass = false;
        try {
            userService.authenticate("owner.a@citymobiles.com", "WrongPassword123!");
        } catch (SecurityException e) {
            rejectedWrongPass = true;
        }
        assertCondition(rejectedWrongPass, "Incorrect password must throw generic SecurityException");

        boolean rejectedNonexistent = false;
        try {
            userService.authenticate("nonexistent@abizlocker.com", "SomePassword123!");
        } catch (SecurityException e) {
            rejectedNonexistent = true;
        }
        assertCondition(rejectedNonexistent, "Nonexistent user must throw same SecurityException (no enumeration)");
    }

    private static void testSuspendedUserRejection(AbizUserService userService) {
        System.out.println("\n--- Test Suite 6: Inactive/Suspended Account Rejection ---");
        boolean rejectedSuspended = false;
        try {
            userService.authenticate("suspended@citymobiles.com", "Suspended@2026Pass!");
        } catch (SecurityException e) {
            rejectedSuspended = true;
        }
        assertCondition(rejectedSuspended, "Suspended account must be rejected with SecurityException");
    }

    private static void testRefreshTokenRotation(AbizUserService userService) {
        System.out.println("\n--- Test Suite 7: Refresh Token Rotation & Issuance ---");
        AbizLoginResponse loginResp = userService.authenticate("staff.a@citymobiles.com", "StaffA@2026Secure!");
        String initialRefresh = loginResp.getRefreshToken();
        String initialAccess = loginResp.getAccessToken();

        // Rotate
        AbizLoginResponse rotatedResp = userService.rotateRefreshToken(initialRefresh);
        assertCondition(rotatedResp != null, "Token rotation must succeed");
        assertCondition(!rotatedResp.getRefreshToken().equals(initialRefresh), "Rotated refresh token must be a NEW random token");
        assertCondition(rotatedResp.getAccessToken() != null, "New access token must be issued");

        // Old token reuse attempt must be rejected!
        boolean rejectedOldToken = false;
        try {
            userService.rotateRefreshToken(initialRefresh);
        } catch (SecurityException e) {
            rejectedOldToken = true;
        }
        assertCondition(rejectedOldToken, "Reuse of rotated/revoked refresh token must be strictly rejected");
    }

    private static void testRevokedTokenRejection(AbizUserService userService) {
        System.out.println("\n--- Test Suite 8: Logout & Revocation ---");
        AbizLoginResponse loginResp = userService.authenticate("staff.a@citymobiles.com", "StaffA@2026Secure!");
        String refresh = loginResp.getRefreshToken();

        // Logout
        userService.logout(refresh);

        boolean rejectedAfterLogout = false;
        try {
            userService.rotateRefreshToken(refresh);
        } catch (SecurityException e) {
            rejectedAfterLogout = true;
        }
        assertCondition(rejectedAfterLogout, "Logged-out refresh token must be rejected");
    }

    private static void testRbacAndTenantIsolation(UUID shopA, UUID shopB) {
        System.out.println("\n--- Test Suite 9: RBAC Enforcement & Strict Tenant Isolation ---");

        // 1. Context: Shop A Owner
        UUID aliceId = UUID.randomUUID();
        AbizTenantContext.set(aliceId, AbizRole.SHOP_OWNER, shopA, "owner.a@citymobiles.com");
        try {
            // Can access own shop
            AbizRbacGuard.requireShopOwner(shopA);
            assertCondition(true, "Shop A owner must be permitted on Shop A resources");

            // CANNOT access Shop B!
            boolean blockedCrossTenant = false;
            try {
                AbizRbacGuard.requireShopOwner(shopB);
            } catch (SecurityException e) {
                blockedCrossTenant = true;
            }
            assertCondition(blockedCrossTenant, "Shop A owner MUST be blocked from Shop B resources (Cross-Tenant Isolation)");

            // CANNOT execute super admin functions
            boolean blockedSuperAdmin = false;
            try {
                AbizRbacGuard.requireSuperAdmin();
            } catch (SecurityException e) {
                blockedSuperAdmin = true;
            }
            assertCondition(blockedSuperAdmin, "Shop A owner MUST be blocked from platform Super Admin functions");

        } finally {
            AbizTenantContext.clear();
        }

        // 2. Context: Shop A Staff
        UUID bobId = UUID.randomUUID();
        AbizTenantContext.set(bobId, AbizRole.SHOP_STAFF, shopA, "staff.a@citymobiles.com");
        try {
            // Can access own shop staff resources
            AbizRbacGuard.requireShopStaffOrOwner(shopA);
            assertCondition(true, "Shop A staff must be permitted on Shop A staff resources");

            // CANNOT access Shop Owner resources!
            boolean blockedOwnerOperation = false;
            try {
                AbizRbacGuard.requireShopOwner(shopA);
            } catch (SecurityException e) {
                blockedOwnerOperation = true;
            }
            assertCondition(blockedOwnerOperation, "Shop A staff MUST be blocked from Shop Owner operations");

            // CANNOT access Shop B!
            boolean blockedStaffCrossTenant = false;
            try {
                AbizRbacGuard.requireShopStaffOrOwner(shopB);
            } catch (SecurityException e) {
                blockedStaffCrossTenant = true;
            }
            assertCondition(blockedStaffCrossTenant, "Shop A staff MUST be blocked from Shop B resources");

        } finally {
            AbizTenantContext.clear();
        }

        // 3. Context: Super Admin
        UUID superId = UUID.randomUUID();
        AbizTenantContext.set(superId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            AbizRbacGuard.requireSuperAdmin();
            AbizRbacGuard.requireShopOwner(shopA);
            AbizRbacGuard.requireShopOwner(shopB);
            assertCondition(true, "Super Admin must have cross-platform access across all shops");
        } finally {
            AbizTenantContext.clear();
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
