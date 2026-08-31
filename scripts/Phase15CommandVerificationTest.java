package com.abiz.locker.test;

import com.abiz.locker.audit.AbizAuditService;
import com.abiz.locker.auth.AbizRole;
import com.abiz.locker.auth.AbizTenantContext;
import com.abiz.locker.command.DeviceCommandService;
import com.abiz.locker.command.transport.MockDeviceCommandTransport;
import com.abiz.locker.contract.ContractService;
import com.abiz.locker.customer.CustomerService;
import com.abiz.locker.device.FinancedDeviceService;
import com.abiz.locker.domain.*;
import com.abiz.locker.installment.InstallmentService;
import com.abiz.locker.mapper.*;
import com.abiz.locker.payment.PaymentService;
import com.abiz.locker.policy.DevicePolicyService;
import com.abiz.locker.shop.ShopService;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 15 — Automated Remote Device Commands & Push Dispatch Verification Test Suite.
 */
public class Phase15CommandVerificationTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:3000/abizlocker";
    private static final String DB_USER = "abizlocker";
    private static final String DB_PASS = "abiz_dev_secure_pass_9281";

    private static SqlSessionFactory factory;
    private static int passedCount = 0;
    private static int failedCount = 0;

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
        System.out.println("  ABIZ LOCKER — PHASE 15 AUTOMATED VERIFICATION TEST SUITE");
        System.out.println("  Remote Device Commands, Push Dispatch & Agent Acknowledgement");
        System.out.println("===============================================================");

        try {
            setupMyBatis();

            try (SqlSession session = factory.openSession(true)) {
                // Initialize Mappers
                ShopMapper shopMapper = session.getMapper(ShopMapper.class);
                ShopUserMapper shopUserMapper = session.getMapper(ShopUserMapper.class);
                CustomerMapper customerMapper = session.getMapper(CustomerMapper.class);
                FinancedDeviceMapper deviceMapper = session.getMapper(FinancedDeviceMapper.class);
                ContractMapper contractMapper = session.getMapper(ContractMapper.class);
                InstallmentMapper installmentMapper = session.getMapper(InstallmentMapper.class);
                PaymentMapper paymentMapper = session.getMapper(PaymentMapper.class);
                DevicePolicyMapper policyMapper = session.getMapper(DevicePolicyMapper.class);
                DeviceCommandMapper commandMapper = session.getMapper(DeviceCommandMapper.class);
                EnrollmentSessionMapper enrollmentMapper = session.getMapper(EnrollmentSessionMapper.class);
                AuditMapper auditMapper = session.getMapper(AuditMapper.class);

                // Audit Service Setup
                AbizAuditService auditService = new AbizAuditService();
                Field f = AbizAuditService.class.getDeclaredField("auditMapper");
                f.setAccessible(true);
                f.set(auditService, auditMapper);

                // Domain Services
                ShopService shopService = new ShopService(shopMapper, auditService);
                CustomerService customerService = new CustomerService(customerMapper, shopMapper, auditService);
                FinancedDeviceService deviceService = new FinancedDeviceService(deviceMapper, customerMapper, auditService);
                ContractService contractService = new ContractService(contractMapper, customerMapper, deviceMapper, auditService);
                InstallmentService installmentService = new InstallmentService(installmentMapper, contractMapper, auditService);
                PaymentService paymentService = new PaymentService(paymentMapper, installmentMapper, auditService);
                DevicePolicyService policyService = new DevicePolicyService(policyMapper, contractMapper, installmentMapper, auditService);

                // Phase 15 Mock Transport & Command Service
                MockDeviceCommandTransport mockTransport = new MockDeviceCommandTransport();
                DeviceCommandService commandService = new DeviceCommandService(commandMapper, policyMapper, mockTransport, auditService);

                // Setup Test Tenancies
                UUID superAdminId = UUID.randomUUID();
                UUID ownerAId = UUID.randomUUID();
                UUID staffAId = UUID.randomUUID();
                UUID ownerBId = UUID.randomUUID();

                AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abiz.com");
                AbizShop shopA = shopService.createShop(new ShopCreateRequest(
                        "P15 Shop Alpha " + UUID.randomUUID().toString().substring(0, 5),
                        "Alpha Owner", "03001112222", "alpha_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Lahore", "Lahore", "ACTIVE"
                ));
                AbizShop shopB = shopService.createShop(new ShopCreateRequest(
                        "P15 Shop Beta " + UUID.randomUUID().toString().substring(0, 5),
                        "Beta Owner", "03003334444", "beta_" + UUID.randomUUID().toString().substring(0, 5) + "@test.com", "Karachi", "Karachi", "ACTIVE"
                ));
                UUID shopAId = shopA.getId();
                UUID shopBId = shopB.getId();

                // Seed Shop Users in abiz_shop_users
                AbizShopUser uOwnerA = new AbizShopUser();
                uOwnerA.setId(ownerAId);
                uOwnerA.setShopId(shopAId);
                uOwnerA.setName("Owner A");
                uOwnerA.setEmail("ownera_" + ownerAId + "@test.com");
                uOwnerA.setPasswordHash("dummyhash");
                uOwnerA.setRole("OWNER");
                uOwnerA.setStatus("ACTIVE");
                shopUserMapper.insertShopUser(uOwnerA);

                AbizShopUser uStaffA = new AbizShopUser();
                uStaffA.setId(staffAId);
                uStaffA.setShopId(shopAId);
                uStaffA.setName("Staff A");
                uStaffA.setEmail("staffa_" + staffAId + "@test.com");
                uStaffA.setPasswordHash("dummyhash");
                uStaffA.setRole("STAFF");
                uStaffA.setStatus("ACTIVE");
                shopUserMapper.insertShopUser(uStaffA);

                AbizShopUser uOwnerB = new AbizShopUser();
                uOwnerB.setId(ownerBId);
                uOwnerB.setShopId(shopBId);
                uOwnerB.setName("Owner B");
                uOwnerB.setEmail("ownerb_" + ownerBId + "@test.com");
                uOwnerB.setPasswordHash("dummyhash");
                uOwnerB.setRole("OWNER");
                uOwnerB.setStatus("ACTIVE");
                shopUserMapper.insertShopUser(uOwnerB);

                AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                AbizCustomer custA = customerService.createCustomer(new CustomerCreateRequest("Customer Alpha", "03001112222", "35202-1500001-1", "Lahore"));

                // Create Enrolled Device in Shop A
                AbizFinancedDevice devA = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "A54", "Samsung"));
                UUID devAId = devA.getId();
                enrollmentMapper.updateDeviceEnrollmentSuccess(devAId, "MDM-DEV-A", "14", "1.2.0", "SN-A", "A54", "Samsung");
                commandService.updateDevicePushToken(devAId, "fcm-token-device-a");

                // Create Enrolled Device in Shop B
                AbizTenantContext.set(ownerBId, AbizRole.SHOP_OWNER, shopBId, "owner.b@test.com");
                AbizCustomer custB = customerService.createCustomer(new CustomerCreateRequest("Customer Beta", "03003334444", "35202-1500002-2", "Karachi"));
                AbizFinancedDevice devB = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Redmi 12", "Xiaomi"));
                UUID devBId = devB.getId();
                enrollmentMapper.updateDeviceEnrollmentSuccess(devBId, "MDM-DEV-B", "13", "1.2.0", "SN-B", "Redmi 12", "Xiaomi");
                commandService.updateDevicePushToken(devBId, "fcm-token-device-b");

                // Switch to Shop Owner A context
                AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");

                // =================================================================
                // Test Suite 1: LOCK Command Creation & Structure
                // =================================================================
                System.out.println("\n--- Test Suite 1: LOCK Command Creation & Structure ---");
                AbizDeviceCommand lockCmd = commandService.createCommand(devAId, "LOCK", "LOCK-KEY-1", null, 60);
                assertNotNull("Lock command created", lockCmd);
                assertEquals("Command type is LOCK", "LOCK", lockCmd.getCommandType());
                assertEquals("Command status is PENDING", "PENDING", lockCmd.getStatus());
                assertEquals("Shop ID matches", shopAId, lockCmd.getShopId());
                assertEquals("Device ID matches", devAId, lockCmd.getFinancedDeviceId());
                assertTrue("Payload contains commandId", lockCmd.getPayload().contains(lockCmd.getId().toString()));
                assertTrue("Payload contains correlationId", lockCmd.getPayload().contains("correlationId"));
                assertNotNull("Expires at is set", lockCmd.getExpiresAt());

                // =================================================================
                // Test Suite 2: UNLOCK Command Creation
                // =================================================================
                System.out.println("\n--- Test Suite 2: UNLOCK Command Creation ---");
                AbizDeviceCommand unlockCmd = commandService.createCommand(devAId, "UNLOCK", "UNLOCK-KEY-1", null, 60);
                assertNotNull("Unlock command created", unlockCmd);
                assertEquals("Command type is UNLOCK", "UNLOCK", unlockCmd.getCommandType());
                assertEquals("Command status is PENDING", "PENDING", unlockCmd.getStatus());

                // =================================================================
                // Test Suite 3: WIPE Command Authorization & RBAC
                // =================================================================
                System.out.println("\n--- Test Suite 3: WIPE Command Authorization & RBAC ---");
                // Shop Owner can create WIPE
                AbizDeviceCommand wipeCmd = commandService.createCommand(devAId, "WIPE", "WIPE-KEY-1", null, 60);
                assertNotNull("Shop Owner can create WIPE", wipeCmd);
                assertEquals("Command type is WIPE", "WIPE", wipeCmd.getCommandType());

                // Shop Staff cannot create WIPE
                AbizTenantContext.set(staffAId, AbizRole.SHOP_STAFF, shopAId, "staff.a@test.com");
                try {
                    commandService.createCommand(devAId, "WIPE", "WIPE-KEY-STAFF", null, 60);
                    fail("Shop Staff must be rejected from creating WIPE command");
                } catch (SecurityException e) {
                    pass("Shop Staff rejected from creating WIPE command (403 Forbidden)");
                }
                AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");

                // =================================================================
                // Test Suite 4: Command Idempotency & Duplicate Active Prevention
                // =================================================================
                System.out.println("\n--- Test Suite 4: Command Idempotency & Duplicate Prevention ---");
                AbizDeviceCommand duplicateLock = commandService.createCommand(devAId, "LOCK", "LOCK-KEY-DUP", null, 60);
                assertEquals("Duplicate active command reuses existing command ID", lockCmd.getId(), duplicateLock.getId());

                // =================================================================
                // Test Suite 5: Push Transport Dispatch Success
                // =================================================================
                System.out.println("\n--- Test Suite 5: Push Transport Dispatch Success ---");
                mockTransport.clearDispatched();
                AbizDeviceCommand dispatchedCmd = commandService.dispatchCommand(lockCmd.getId());
                assertEquals("Command status transitioned to SENT", "SENT", dispatchedCmd.getStatus());
                assertNotNull("Dispatched timestamp recorded", dispatchedCmd.getDispatchedAt());
                List<AbizDeviceCommand> dispatchedList = mockTransport.getDispatchedCommandsForDevice(devAId);
                assertEquals("Mock transport received 1 command", 1, dispatchedList.size());
                assertEquals("Transport received correct command ID", lockCmd.getId(), dispatchedList.get(0).getId());

                // =================================================================
                // Test Suite 6: Push Transport Dispatch Failure & Transient Retry
                // =================================================================
                System.out.println("\n--- Test Suite 6: Push Transport Failure & Transient Retry ---");
                mockTransport.setSimulateFailure(true, true);
                AbizDeviceCommand retryableCmd = commandService.createCommand(devAId, "PAYMENT_WARNING", "WARN-KEY-1", null, 60);
                AbizDeviceCommand failedAttempt1 = commandService.dispatchCommand(retryableCmd.getId());
                assertEquals("Transient failure keeps status PENDING for retry", "PENDING", failedAttempt1.getStatus());
                assertEquals("Retry count is 1", 1, failedAttempt1.getRetryCount());
                assertTrue("Failure reason recorded", failedAttempt1.getFailureReason().contains("Simulated transport network failure"));

                // =================================================================
                // Test Suite 7: Max Retry Exhaustion
                // =================================================================
                System.out.println("\n--- Test Suite 7: Max Retry Exhaustion ---");
                mockTransport.setSimulateFailure(true, false); // Fatal failure
                AbizDeviceCommand fatalFailed = commandService.dispatchCommand(retryableCmd.getId());
                assertEquals("Non-retryable failure transitions status to FAILED", "FAILED", fatalFailed.getStatus());
                mockTransport.setSimulateFailure(false, false); // Reset mock

                // =================================================================
                // Test Suite 8: Android Agent Acknowledgement & Lock Execution
                // =================================================================
                System.out.println("\n--- Test Suite 8: Agent Acknowledgement & Lock Execution ---");
                AbizDeviceCommand executedLock = commandService.acknowledgeCommand(
                        lockCmd.getId(),
                        new DeviceCommandAckRequest(lockCmd.getId(), devAId, "SUCCESS", "EXECUTED", "Device screen locked successfully", "1.2.0")
                );
                assertEquals("Command status is EXECUTED", "EXECUTED", executedLock.getStatus());
                assertNotNull("Executed timestamp recorded", executedLock.getExecutedAt());
                assertEquals("Agent version recorded", "1.2.0", executedLock.getAgentVersion());

                // Verify physical device state updated
                AbizFinancedDevice lockedDev = deviceMapper.findDeviceById(devAId);
                assertEquals("Physical device lock_status updated to LOCKED", "LOCKED", lockedDev.getLockStatus());

                // =================================================================
                // Test Suite 9: Replay Safety (Duplicate ACK Handling)
                // =================================================================
                System.out.println("\n--- Test Suite 9: Replay Safety (Duplicate ACK Handling) ---");
                AbizDeviceCommand replayAck = commandService.acknowledgeCommand(
                        lockCmd.getId(),
                        new DeviceCommandAckRequest(lockCmd.getId(), devAId, "SUCCESS", "EXECUTED", "Duplicate ACK replay", "1.2.0")
                );
                assertEquals("Duplicate ACK safely handled idempotently", "EXECUTED", replayAck.getStatus());

                // =================================================================
                // Test Suite 10: Wrong Device Acknowledgement Rejection
                // =================================================================
                System.out.println("\n--- Test Suite 10: Wrong Device Acknowledgement Rejection ---");
                try {
                    commandService.acknowledgeCommand(
                            unlockCmd.getId(),
                            new DeviceCommandAckRequest(unlockCmd.getId(), devBId, "SUCCESS", "EXECUTED", "Wrong device ACK", "1.2.0")
                    );
                    fail("Acknowledgement from wrong device must be rejected");
                } catch (SecurityException e) {
                    pass("Acknowledgement with mismatched device ID rejected (403 Forbidden)");
                }

                // =================================================================
                // Test Suite 11: UNLOCK Command Execution & Physical Unlock
                // =================================================================
                System.out.println("\n--- Test Suite 11: UNLOCK Execution & Physical Unlock ---");
                commandService.dispatchCommand(unlockCmd.getId());
                AbizDeviceCommand executedUnlock = commandService.acknowledgeCommand(
                        unlockCmd.getId(),
                        new DeviceCommandAckRequest(unlockCmd.getId(), devAId, "SUCCESS", "EXECUTED", "Lock screen dismissed", "1.2.0")
                );
                assertEquals("Unlock status is EXECUTED", "EXECUTED", executedUnlock.getStatus());
                AbizFinancedDevice unlockedDev = deviceMapper.findDeviceById(devAId);
                assertEquals("Physical device lock_status updated to UNLOCKED", "UNLOCKED", unlockedDev.getLockStatus());

                // =================================================================
                // Test Suite 12: Push Token Registration & Rotation
                // =================================================================
                System.out.println("\n--- Test Suite 12: Push Token Registration & Rotation ---");
                commandService.updateDevicePushToken(devAId, "fcm-token-rotated-999");
                AbizFinancedDevice devWithRotatedToken = deviceMapper.findDeviceById(devAId);
                assertEquals("Push token rotated in database", "fcm-token-rotated-999", devWithRotatedToken.getPushToken());

                // =================================================================
                // Test Suite 13: Missing & Invalid Push Token Handling
                // =================================================================
                System.out.println("\n--- Test Suite 13: Missing & Invalid Push Token Handling ---");
                commandService.updateDevicePushToken(devAId, "INVALID_TOKEN");
                AbizDeviceCommand invalidTokenCmd = commandService.createCommand(devAId, "LOCK", "INV-KEY-1", null, 60);
                AbizDeviceCommand failedInvToken = commandService.dispatchCommand(invalidTokenCmd.getId());
                assertEquals("Dispatch rejected due to invalid push token", "FAILED", failedInvToken.getStatus());
                assertTrue("Failure reason mentions invalid token", failedInvToken.getFailureReason().contains("Invalid push"));

                // Restore valid push token
                commandService.updateDevicePushToken(devAId, "fcm-token-device-a");

                // =================================================================
                // Test Suite 14: Tenant Isolation & RBAC Protection
                // =================================================================
                System.out.println("\n--- Test Suite 14: Tenant Isolation & RBAC Protection ---");
                // Shop Owner A accessing Shop B device
                try {
                    commandService.createCommand(devBId, "LOCK", "CROSS-TENANT-KEY", null, 60);
                    fail("Cross-tenant command creation must be blocked");
                } catch (SecurityException e) {
                    pass("Cross-tenant command creation rejected (403 Forbidden)");
                }

                // Super Admin accessing Shop B device
                AbizTenantContext.set(superAdminId, AbizRole.SUPER_ADMIN, null, "superadmin@abiz.com");
                AbizDeviceCommand adminCmd = commandService.createCommand(devBId, "LOCK", "ADMIN-LOCK-B", null, 60);
                assertNotNull("Super Admin can manage commands across all shops", adminCmd);
                AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");

                // =================================================================
                // Test Suite 15: Unenrolled Device Protection
                // =================================================================
                System.out.println("\n--- Test Suite 15: Unenrolled Device Protection ---");
                AbizFinancedDevice unenrolledDev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "G20", "Nokia"));
                try {
                    commandService.createCommand(unenrolledDev.getId(), "LOCK", "UNENR-KEY", null, 60);
                    fail("Command to unenrolled device must be rejected");
                } catch (IllegalStateException e) {
                    pass("Command to unenrolled device rejected: " + e.getMessage());
                }

                // =================================================================
                // Test Suite 16: Policy State Machine -> Remote Command Integration
                // =================================================================
                System.out.println("\n--- Test Suite 16: Policy Engine -> Remote Command Integration ---");
                // Setup Contract with Overdue Installment
                AbizFinancedDevice financedDev = deviceService.registerDevice(new DeviceRegisterRequest(nextValidImei(), "Hot 40", "Infinix"));
                enrollmentMapper.updateDeviceEnrollmentSuccess(financedDev.getId(), "MDM-DEV-77", "14", "1.2.0", "SN-77", "Hot 40", "Infinix");
                commandService.updateDevicePushToken(financedDev.getId(), "fcm-token-financed-77");

                Date overdueDue = Date.from(Instant.now().minus(20, ChronoUnit.DAYS));
                ContractCreateRequest contractReq = new ContractCreateRequest();
                contractReq.setCustomerId(custA.getId());
                contractReq.setFinancedDeviceId(financedDev.getId());
                contractReq.setDevicePrice(new BigDecimal("50000.00"));
                contractReq.setDownPayment(new BigDecimal("10000.00"));
                contractReq.setFinancedAmount(new BigDecimal("40000.00"));
                contractReq.setInstallmentAmount(new BigDecimal("20000.00"));
                contractReq.setNumberOfInstallments(2);
                contractReq.setFrequency("MONTHLY");
                contractReq.setGracePeriodDays(3);
                contractReq.setFirstDueDate(overdueDue);
                contractReq.setNotes("Policy Command Integration Test Contract");

                AbizContract contract = contractService.createContract(contractReq);
                contractService.activateContract(contract.getId());
                installmentService.generateSchedule(contract.getId());

                // Evaluate policy -> Produces LOCKED
                PolicyEvaluationResult policyRes = policyService.evaluateDevicePolicy(financedDev.getId());
                assertEquals("Policy state is LOCKED", "LOCKED", policyRes.getNewPolicyState());

                // Phase 15 automated command trigger
                AbizDeviceCommand autoLock = commandService.processPolicyCommand(financedDev.getId(), policyRes.getNewPolicyState(), policyRes.getNewLockStatus());
                assertNotNull("Automated LOCK command generated", autoLock);
                assertEquals("Auto command type is LOCK", "LOCK", autoLock.getCommandType());
                assertEquals("Auto command dispatched (status SENT)", "SENT", autoLock.getStatus());

                // Agent executes auto lock
                commandService.acknowledgeCommand(
                        autoLock.getId(),
                        new DeviceCommandAckRequest(autoLock.getId(), financedDev.getId(), "SUCCESS", "EXECUTED", "Auto lock executed", "1.2.0")
                );
                AbizFinancedDevice autoLockedDev = deviceMapper.findDeviceById(financedDev.getId());
                assertEquals("Physical lock status confirmed LOCKED", "LOCKED", autoLockedDev.getLockStatus());

                // Pay overdue installment 1 balance -> Recovery to ACTIVE
                paymentService.recordPayment(new PaymentRecordRequest(contract.getId(), null, new BigDecimal("20000.00"), "CASH", "REC-PAY-1", "Overdue installment payment"));
                PolicyEvaluationResult recRes = policyService.evaluateDevicePolicy(financedDev.getId());
                assertEquals("Policy recovered to ACTIVE", "ACTIVE", recRes.getNewPolicyState());

                // Automated UNLOCK command trigger
                AbizDeviceCommand autoUnlock = commandService.processPolicyCommand(financedDev.getId(), recRes.getNewPolicyState(), recRes.getNewLockStatus());
                assertNotNull("Automated UNLOCK command generated", autoUnlock);
                assertEquals("Auto command type is UNLOCK", "UNLOCK", autoUnlock.getCommandType());

                // Agent executes auto unlock
                commandService.acknowledgeCommand(
                        autoUnlock.getId(),
                        new DeviceCommandAckRequest(autoUnlock.getId(), financedDev.getId(), "SUCCESS", "EXECUTED", "Auto unlock executed", "1.2.0")
                );
                AbizFinancedDevice autoUnlockedDev = deviceMapper.findDeviceById(financedDev.getId());
                assertEquals("Physical lock status confirmed UNLOCKED", "UNLOCKED", autoUnlockedDev.getLockStatus());

                // =================================================================
                // Test Suite 17: Command Cancellation & Retry API
                // =================================================================
                System.out.println("\n--- Test Suite 17: Command Cancellation & Retry API ---");
                AbizDeviceCommand toCancel = commandService.createCommand(devAId, "PAYMENT_WARNING", "TO-CANCEL-1", null, 60);
                AbizDeviceCommand cancelled = commandService.cancelCommand(toCancel.getId(), "Customer visited shop");
                assertEquals("Command status is CANCELLED", "CANCELLED", cancelled.getStatus());
                assertTrue("Failure reason notes cancellation", cancelled.getFailureReason().contains("Cancelled"));

                // =================================================================
                // Test Suite 18: Multi-Threaded Concurrency (10 Threads Create LOCK)
                // =================================================================
                System.out.println("\n--- Test Suite 18: 10-Thread Concurrent Command Creation ---");
                int threads = 10;
                ExecutorService executor = Executors.newFixedThreadPool(threads);
                CountDownLatch latch = new CountDownLatch(1);
                List<Future<UUID>> futures = new ArrayList<>();

                for (int i = 0; i < threads; i++) {
                    futures.add(executor.submit(() -> {
                        latch.await();
                        try (SqlSession threadSession = factory.openSession(true)) {
                            DeviceCommandMapper tm = threadSession.getMapper(DeviceCommandMapper.class);
                            DevicePolicyMapper pm = threadSession.getMapper(DevicePolicyMapper.class);
                            AuditMapper am = threadSession.getMapper(AuditMapper.class);

                            AbizAuditService tAudit = new AbizAuditService();
                            Field af = AbizAuditService.class.getDeclaredField("auditMapper");
                            af.setAccessible(true);
                            af.set(tAudit, am);

                            DeviceCommandService tcService = new DeviceCommandService(tm, pm, mockTransport, tAudit);
                            AbizTenantContext.set(ownerAId, AbizRole.SHOP_OWNER, shopAId, "owner.a@test.com");
                            AbizDeviceCommand c = tcService.createCommand(devAId, "LOCK", "CONCURRENT-LOCK", null, 60);
                            return c.getId();
                        } finally {
                            AbizTenantContext.clear();
                        }
                    }));
                }

                latch.countDown();
                Set<UUID> commandIds = new HashSet<>();
                for (Future<UUID> fut : futures) {
                    commandIds.add(fut.get(10, TimeUnit.SECONDS));
                }
                executor.shutdown();

                assertEquals("All 10 concurrent requests returned EXACTLY 1 active logical command ID", 1, commandIds.size());

                // =================================================================
                // Test Suite 19: Multi-Threaded Concurrency (10 Threads Acknowledge)
                // =================================================================
                System.out.println("\n--- Test Suite 19: 10-Thread Concurrent Acknowledgement ---");
                UUID targetCommandId = commandIds.iterator().next();
                ExecutorService ackExecutor = Executors.newFixedThreadPool(threads);
                CountDownLatch ackLatch = new CountDownLatch(1);
                List<Future<String>> ackFutures = new ArrayList<>();

                for (int i = 0; i < threads; i++) {
                    ackFutures.add(ackExecutor.submit(() -> {
                        ackLatch.await();
                        try (SqlSession threadSession = factory.openSession(true)) {
                            DeviceCommandMapper tm = threadSession.getMapper(DeviceCommandMapper.class);
                            DevicePolicyMapper pm = threadSession.getMapper(DevicePolicyMapper.class);
                            AuditMapper am = threadSession.getMapper(AuditMapper.class);

                            AbizAuditService tAudit = new AbizAuditService();
                            Field af = AbizAuditService.class.getDeclaredField("auditMapper");
                            af.setAccessible(true);
                            af.set(tAudit, am);

                            DeviceCommandService tcService = new DeviceCommandService(tm, pm, mockTransport, tAudit);
                            AbizDeviceCommand c = tcService.acknowledgeCommand(
                                    targetCommandId,
                                    new DeviceCommandAckRequest(targetCommandId, devAId, "SUCCESS", "EXECUTED", "Concurrent ACK", "1.2.0")
                            );
                            return c.getStatus();
                        }
                    }));
                }

                ackLatch.countDown();
                for (Future<String> fut : ackFutures) {
                    assertEquals("All concurrent ACKs completed with EXECUTED", "EXECUTED", fut.get(10, TimeUnit.SECONDS));
                }
                ackExecutor.shutdown();

                // =================================================================
                // Test Suite 20: Audit Trail Verification for Command Operations
                // =================================================================
                System.out.println("\n--- Test Suite 20: Audit Trail Verification for Command Operations ---");
                verifyAuditLogs();
            }

            System.out.println("\n===============================================================");
            System.out.println("PHASE 15 TEST RESULTS: " + passedCount + " PASSED, " + failedCount + " FAILED");
            System.out.println("===============================================================");

            if (failedCount > 0) {
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("FATAL TEST EXECUTION ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void setupMyBatis() {
        PooledDataSource dataSource = new PooledDataSource(
                "org.postgresql.Driver",
                DB_URL,
                DB_USER,
                DB_PASS
        );
        Environment environment = new Environment("development", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.getTypeHandlerRegistry().register(com.abiz.locker.mapper.UuidTypeHandler.class);
        configuration.addMapper(ShopMapper.class);
        configuration.addMapper(ShopUserMapper.class);
        configuration.addMapper(CustomerMapper.class);
        configuration.addMapper(FinancedDeviceMapper.class);
        configuration.addMapper(ContractMapper.class);
        configuration.addMapper(InstallmentMapper.class);
        configuration.addMapper(PaymentMapper.class);
        configuration.addMapper(DevicePolicyMapper.class);
        configuration.addMapper(DeviceCommandMapper.class);
        configuration.addMapper(EnrollmentSessionMapper.class);
        configuration.addMapper(AuditMapper.class);

        factory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private static void verifyAuditLogs() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT action, result, COUNT(*) as count FROM abiz_audit_logs " +
                             "WHERE action IN ('DEVICE_COMMAND_CREATED', 'DEVICE_COMMAND_DISPATCHED', " +
                             "'DEVICE_COMMAND_ACKNOWLEDGED', 'DEVICE_COMMAND_EXECUTED', 'DEVICE_COMMAND_FAILED', " +
                             "'DEVICE_COMMAND_CANCELLED', 'DEVICE_LOCK_REQUESTED', 'DEVICE_UNLOCK_REQUESTED', " +
                             "'DEVICE_WIPE_REQUESTED', 'CROSS_TENANT_COMMAND_ACCESS_BLOCKED', 'UNAUTHORIZED_WIPE_ATTEMPT') " +
                             "GROUP BY action, result ORDER BY action")) {
            ResultSet rs = ps.executeQuery();
            int totalLogged = 0;
            while (rs.next()) {
                String action = rs.getString("action");
                String result = rs.getString("result");
                int count = rs.getInt("count");
                totalLogged += count;
                System.out.println("  [AUDIT LOGGED] action=" + action + ", result=" + result + ", count=" + count);
            }
            assertTrue("Command operational events recorded in abiz_audit_logs (Total: " + totalLogged + ")", totalLogged >= 10);
        }
    }

    private static void pass(String msg) {
        passedCount++;
        System.out.println("  [PASS] " + msg);
    }

    private static void fail(String msg) {
        failedCount++;
        System.err.println("  [FAIL] " + msg);
    }

    private static void assertTrue(String msg, boolean condition) {
        if (condition) pass(msg); else fail(msg);
    }

    private static void assertEquals(String msg, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            pass(msg + " (Got: " + actual + ")");
        } else {
            fail(msg + " (Expected: " + expected + ", Got: " + actual + ")");
        }
    }

    private static void assertNotNull(String msg, Object obj) {
        if (obj != null) pass(msg); else fail(msg + " (Expected non-null)");
    }
}
