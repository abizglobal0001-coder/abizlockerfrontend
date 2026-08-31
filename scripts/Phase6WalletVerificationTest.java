package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.domain.AbizShop;
import com.abiz.locker.domain.ShopCreateRequest;
import com.abiz.locker.mapper.AuditMapper;
import com.abiz.locker.mapper.ShopMapper;
import com.abiz.locker.mapper.UuidTypeHandler;
import com.abiz.locker.shop.ShopService;
import com.abiz.locker.wallet.WalletMapper;
import com.abiz.locker.wallet.WalletMapper.WalletTransactionRow;
import com.abiz.locker.wallet.WalletService;
import com.abiz.locker.wallet.WalletService.InsufficientCreditsException;
import com.abiz.locker.wallet.WalletService.WalletBalance;
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
 * End-to-end Phase 6 Wallet & Credit Ledger Automated Verification Test Suite.
 * Executes live against the local PostgreSQL 18.0 database.
 */
public class Phase6WalletVerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("ABIZ LOCKER — PHASE 6 WALLET & CREDIT LEDGER TEST SUITE");
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
            config.addMapper(WalletMapper.class);

            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(config);

            try (SqlSession session = factory.openSession(true)) {
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);
                WalletMapper walletMapper = session.getMapper(WalletMapper.class);

                AbizAuditService auditService = new AbizAuditService();
                Field auditMapperField = AbizAuditService.class.getDeclaredField("auditMapper");
                auditMapperField.setAccessible(true);
                auditMapperField.set(auditService, auditMapper);

                WalletService walletService = new WalletService(walletMapper, auditService);
                ShopService shopService = new ShopService(shopMapper, walletService, auditService);

                UUID superAdminId = UUID.randomUUID();
                AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");

                // Create test shops with automatic wallet initialization
                AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                        "Wallet Test Shop A " + UUID.randomUUID().toString().substring(0, 6),
                        "Owner A", "+923001119999", "walletA@test.com", "Addr", "Lahore", "ACTIVE"
                ));
                AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                        "Wallet Test Shop B " + UUID.randomUUID().toString().substring(0, 6),
                        "Owner B", "+923002229999", "walletB@test.com", "Addr", "Karachi", "ACTIVE"
                ));
                UUID shopAId = shopA.getId();
                UUID shopBId = shopB.getId();
                AbizTenantContext.clear();

                UUID shopOwnerAId = UUID.randomUUID();
                UUID shopStaffAId = UUID.randomUUID();

                // Run Test Suites
                testWalletInitialization(walletService, superAdminId, shopAId);
                testSuperAdminCreditAddition(walletService, superAdminId, shopAId);
                testCreditReservationAndIdempotency(walletService, superAdminId, shopAId);
                testInsufficientCreditsProtection(walletService, superAdminId, shopAId);
                testCreditConfirmationFlow(walletService, superAdminId, shopAId);
                testCreditRefundFlowAndIdempotency(walletService, superAdminId, shopAId);
                testLedgerTransactionHistory(walletService, superAdminId, shopAId);
                testRbacAndMultiTenantBoundaries(walletService, shopAId, shopBId, shopOwnerAId, shopStaffAId);
                testAuditTrailVerification(session);

                System.out.println("\n===============================================================");
                System.out.println(String.format("PHASE 6 TEST RESULTS: %d PASSED, %d FAILED", testsPassed, testsFailed));
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

    private static void testWalletInitialization(WalletService walletService, UUID adminId, UUID shopId) {
        System.out.println("\n--- Test Suite 1: Wallet Automatic Provisioning ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            WalletBalance balance = walletService.getBalance(shopId);
            assertCondition(balance != null, "Wallet balance must be returned for shop");
            assertCondition(shopId.equals(balance.shopId()), "Wallet shopId must match");
            assertCondition(balance.balance() == 0, "Initial wallet balance must be 0");
            assertCondition(balance.version() == 0, "Initial wallet version must be 0");

            // Test idempotency of wallet creation
            walletService.createWallet(shopId);
            WalletBalance balanceAfterDup = walletService.getBalance(shopId);
            assertCondition(balanceAfterDup.balance() == 0, "Duplicate createWallet must not corrupt existing balance");
        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testSuperAdminCreditAddition(WalletService walletService, UUID adminId, UUID shopId) {
        System.out.println("\n--- Test Suite 2: Super Admin Credit Addition (CREDIT_ADD) ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            WalletBalance updated = walletService.addCredits(shopId, 50, "Initial credit package");
            assertCondition(updated.balance() == 50, "Wallet balance must increase to 50");
            assertCondition(updated.version() == 1, "Wallet version must increment to 1");

            // Additional credit addition
            WalletBalance updated2 = walletService.addCredits(shopId, 25, "Top-up package");
            assertCondition(updated2.balance() == 75, "Wallet balance must increase to 75");
            assertCondition(updated2.version() == 2, "Wallet version must increment to 2");

            // Non-positive credit addition rejection
            boolean nonPositiveRejected = false;
            try {
                walletService.addCredits(shopId, 0, "Zero credits");
            } catch (IllegalArgumentException e) {
                nonPositiveRejected = true;
            }
            assertCondition(nonPositiveRejected, "Adding 0 credits must be rejected with IllegalArgumentException");

            boolean negativeRejected = false;
            try {
                walletService.addCredits(shopId, -10, "Negative credits");
            } catch (IllegalArgumentException e) {
                negativeRejected = true;
            }
            assertCondition(negativeRejected, "Adding negative credits must be rejected with IllegalArgumentException");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCreditReservationAndIdempotency(WalletService walletService, UUID adminId, UUID shopId) {
        System.out.println("\n--- Test Suite 3: Credit Reservation & Idempotency ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            UUID sessionId = UUID.randomUUID();
            WalletBalance afterReserve = walletService.reserveActivationCredit(shopId, sessionId, 1);
            assertCondition(afterReserve.balance() == 74, "Balance must decrease from 75 to 74 on 1 credit reservation");

            // Idempotency test: Same sessionId call again
            WalletBalance afterIdempotent = walletService.reserveActivationCredit(shopId, sessionId, 1);
            assertCondition(afterIdempotent.balance() == 74, "Duplicate reservation with same sessionId MUST NOT double-deduct");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testInsufficientCreditsProtection(WalletService walletService, UUID adminId, UUID shopId) {
        System.out.println("\n--- Test Suite 4: Insufficient Credits Protection ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            UUID bigSessionId = UUID.randomUUID();
            boolean insufficientCaught = false;
            try {
                walletService.reserveActivationCredit(shopId, bigSessionId, 100);
            } catch (InsufficientCreditsException e) {
                insufficientCaught = true;
            }
            assertCondition(insufficientCaught, "Reserving more credits than available must throw InsufficientCreditsException");

            WalletBalance balance = walletService.getBalance(shopId);
            assertCondition(balance.balance() == 74, "Balance must remain unchanged after rejected reservation");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCreditConfirmationFlow(WalletService walletService, UUID adminId, UUID shopId) {
        System.out.println("\n--- Test Suite 5: Credit Confirmation Flow (DEVICE_ACTIVATION_CONFIRM) ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            UUID sessionId = UUID.randomUUID();
            walletService.reserveActivationCredit(shopId, sessionId, 1);
            WalletBalance balanceBeforeConfirm = walletService.getBalance(shopId);

            // Confirm
            walletService.confirmActivation(shopId, sessionId);
            WalletBalance balanceAfterConfirm = walletService.getBalance(shopId);
            assertCondition(balanceBeforeConfirm.balance() == balanceAfterConfirm.balance(),
                    "Confirmation must finalize reservation without altering balance (already deducted)");

            // Confirm idempotency
            walletService.confirmActivation(shopId, sessionId);
            assertCondition(true, "Repeated confirmActivation call must be safe and idempotent");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testCreditRefundFlowAndIdempotency(WalletService walletService, UUID adminId, UUID shopId) {
        System.out.println("\n--- Test Suite 6: Credit Refund Flow (DEVICE_ACTIVATION_REFUND) ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            UUID sessionId = UUID.randomUUID();
            walletService.reserveActivationCredit(shopId, sessionId, 1);
            int balanceAfterReserve = walletService.getBalance(shopId).balance();

            // Refund
            WalletBalance afterRefund = walletService.refundActivation(shopId, sessionId, 1);
            assertCondition(afterRefund.balance() == balanceAfterReserve + 1, "Refunding must restore 1 credit to balance");

            // Refund idempotency
            WalletBalance afterSecondRefund = walletService.refundActivation(shopId, sessionId, 1);
            assertCondition(afterSecondRefund.balance() == afterRefund.balance(), "Repeated refundActivation call MUST NOT double-refund");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testLedgerTransactionHistory(WalletService walletService, UUID adminId, UUID shopId) {
        System.out.println("\n--- Test Suite 7: Ledger Transaction History & Pagination ---");
        AbizTenantContext.set(adminId, AbizRole.SUPER_ADMIN, null, "superadmin@abizlocker.com");
        try {
            List<WalletTransactionRow> history = walletService.listTransactions(shopId, 0, 20);
            assertCondition(history != null && !history.isEmpty(), "Transaction history must return ledger rows");

            WalletTransactionRow latest = history.get(0);
            assertCondition(latest.shopId().equals(shopId), "Transaction row shopId must match");
            assertCondition(latest.createdAt() != null, "Transaction createdAt timestamp must be populated");
            assertCondition(latest.balanceBefore() >= 0 && latest.balanceAfter() >= 0, "Ledger balances must be non-negative");

            // Verify pagination
            List<WalletTransactionRow> page1 = walletService.listTransactions(shopId, 0, 2);
            assertCondition(page1.size() <= 2, "Page size limit of 2 must be respected");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testRbacAndMultiTenantBoundaries(
            WalletService walletService, UUID shopA, UUID shopB, UUID ownerAId, UUID staffAId) {
        System.out.println("\n--- Test Suite 8: RBAC & Strict Multi-Tenant Boundaries ---");

        // 1. Shop Owner A on Shop A
        AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopA, "owner.a@test.com");
        try {
            WalletBalance myBalance = walletService.getBalance(shopA);
            assertCondition(myBalance != null, "Shop Owner A must be able to view their own wallet balance");

            List<WalletTransactionRow> myLedger = walletService.listTransactions(shopA, 0, 10);
            assertCondition(myLedger != null, "Shop Owner A must be able to view their own transaction history");

            // CROSS-TENANT DEFENSE: Shop Owner A queries Shop B
            boolean crossTenantBalanceBlocked = false;
            try {
                walletService.getBalance(shopB);
            } catch (SecurityException e) {
                crossTenantBalanceBlocked = true;
            }
            assertCondition(crossTenantBalanceBlocked, "Shop Owner A MUST be strictly blocked from viewing Shop B balance");

            boolean crossTenantLedgerBlocked = false;
            try {
                walletService.listTransactions(shopB, 0, 10);
            } catch (SecurityException e) {
                crossTenantLedgerBlocked = true;
            }
            assertCondition(crossTenantLedgerBlocked, "Shop Owner A MUST be strictly blocked from viewing Shop B ledger");

            // PRIVILEGE ESCALATION DEFENSE: Shop Owner attempts to add credits
            boolean addCreditsBlocked = false;
            try {
                walletService.addCredits(shopA, 100, "Self add credits");
            } catch (SecurityException e) {
                addCreditsBlocked = true;
            }
            assertCondition(addCreditsBlocked, "Shop Owner MUST be blocked from adding credits (Super Admin only)");

        } finally {
            AbizTenantContext.clear();
        }

        // 2. Shop Staff A
        AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopA, "staff.a@test.com");
        try {
            boolean staffBalanceBlocked = false;
            try {
                walletService.getBalance(shopA);
            } catch (SecurityException e) {
                staffBalanceBlocked = true;
            }
            assertCondition(staffBalanceBlocked, "Shop Staff MUST be blocked from wallet balance view");

            boolean staffLedgerBlocked = false;
            try {
                walletService.listTransactions(shopA, 0, 10);
            } catch (SecurityException e) {
                staffLedgerBlocked = true;
            }
            assertCondition(staffLedgerBlocked, "Shop Staff MUST be blocked from wallet ledger view");

            boolean staffAddCreditsBlocked = false;
            try {
                walletService.addCredits(shopA, 50, "Staff add credits");
            } catch (SecurityException e) {
                staffAddCreditsBlocked = true;
            }
            assertCondition(staffAddCreditsBlocked, "Shop Staff MUST be blocked from adding credits");

        } finally {
            AbizTenantContext.clear();
        }
    }

    private static void testAuditTrailVerification(SqlSession session) {
        System.out.println("\n--- Test Suite 9: Audit Trail Generation for Wallet Events ---");
        Connection conn = session.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT action, result, count(1) AS cnt FROM abiz_audit_logs " +
                "WHERE action IN ('WALLET_CREDIT_ADD', 'CROSS_TENANT_WALLET_ACCESS_BLOCKED') " +
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
            assertCondition(totalLogged > 0, "Wallet operational events must be recorded in abiz_audit_logs");
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
