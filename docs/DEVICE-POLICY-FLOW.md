# ABIZ LOCKER — Device Policy & Locking Enforcement Flow

## 1. Executive Architecture Principle

> **CRITICAL ARCHITECTURAL GUARANTEE:**
> In ABIZ Locker, device locking is **NEVER** simply a cosmetic database status or a standard app screen that can be swiped away.
> It is an **active Android Enterprise Device Owner (DO) enforcement mechanism** that leverages hardware-level and system-level APIs.

---

## 2. Policy State Transitions

```
[Contract Active]
       |
       v
+--------------+      3 Days Before Due
|    ACTIVE    | ------------------------> [ WARNING ]
+--------------+                              |
       ^                                      | Displays persistent non-blocking
       | Payment Received                     | notification and payment banner
       +--------------------------------------+
       |
       | Due Date Passes (Unpaid)
       v
+--------------+      Grace Period Expired (Unpaid)
| GRACE_PERIOD | ------------------------> [ OVERDUE ]
+--------------+                              |
       ^                                      | Escalated warning dialogs,
       | Payment Received                     | periodic lock-prompt reminders
       +--------------------------------------+
       |
       | Overdue Threshold Exceeded (e.g. +7 Days)
       v
+--------------+
|    LOCKED    |  <=== Full Device Lockout Enforced by Android Device Owner
+--------------+
       |
       | Shop records full/agreed installment payment
       | Verified in backend ledger -> Policy State: ACTIVE
       v
  [ UNLOCKED ]  <=== Hardware & UI lockout restrictions lifted
```

---

## 3. What Actually Happens When a Device is "LOCKED"

When `PolicyStateMachine.transitionToLocked(deviceId, shopId)` triggers:

### Server Side:
1. Validates device belongs to shop and is in `OVERDUE` state.
2. Updates `abiz_financed_devices.lock_status = 'LOCKED'`.
3. Inserts state history into `abiz_device_policy_states`.
4. Enqueues an `ABIZ_LOCK` command into the MDMesh `agentCommand` pipeline.
5. Emits audit log entry `DEVICE_LOCK_COMMAND`.

### Android Agent Side (on receipt of command or local policy evaluation):
1. **Immediate Screen Lock**:
   Calls `DevicePolicyManager.lockNow()`. This instantly locks the keyguard.
2. **Kiosk Lock Screen Mode**:
   Launches `AbizLockedActivity` configured with `FLAG_SHOW_WHEN_LOCKED`, `FLAG_TURN_SCREEN_ON`, and `FLAG_DISMISS_KEYGUARD`.
   The window is set to full-screen with touch event consumption over all unauthorized UI elements.
3. **Hardware & System Restrictions Applied**:
   - `DevicePolicyManager.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)` (Anti-tamper)
   - `DevicePolicyManager.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)` (Prevents safe-mode bypass)
   - `DevicePolicyManager.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)`
   - `DevicePolicyManager.addUserRestriction(admin, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)`
   - `DevicePolicyManager.setKeyguardDisabledFeatures(admin, KEYGUARD_DISABLE_FEATURES_ALL)`
   - `DevicePolicyManager.setStatusBarDisabled(admin, true)` (Prevents pulling down status bar / quick settings)
4. **Emergency & Shop Contact Call Whitelist**:
   - Outgoing calls to standard emergency numbers (112, 911, 15) remain strictly allowed by law.
   - A single prominent "Call Shop Owner" button is enabled with direct dial intent to the shop phone.
5. **Offline Autonomous Persistence**:
   - The lock status is committed to encrypted DataStore.
   - A boot receiver re-applies the lock immediately on system reboot even if SIM card is removed or Wi-Fi is disconnected.
