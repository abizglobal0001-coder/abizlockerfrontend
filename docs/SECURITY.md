# ABIZ LOCKER — Security, Threat Modeling & Compliance Architecture

## 1. Threat Model & Mitigations

| Threat | Attack Vector | Mitigation in ABIZ Locker |
|--------|---------------|---------------------------|
| **Factory Reset Bypass** | Customer reboots into recovery or settings to wipe lock | `UserManager.DISALLOW_FACTORY_RESET` enforced by Device Owner. Recovery mode blocked where OEM APIs support it. |
| **Safe Mode Bypass** | Customer boots holding volume keys to disable 3rd party apps | `UserManager.DISALLOW_SAFE_BOOT` enabled. Device Owner continues running in safe mode or re-asserts on boot. |
| **SIM Card Removal / Airplane Mode** | Customer disconnects phone to prevent remote lock commands | Offline policy enforcement: Lock state persists in hardware/DataStore; local policy timer triggers warning/lockout if offline too long. |
| **USB Debugging / ADB Tamper** | Technical user attempts `adb shell pm uninstall` | Device Owner packages **cannot** be uninstalled via ADB or settings without factory wipe. `DISALLOW_DEBUGGING_FEATURES` applied in locked mode. |
| **App Task Killing** | User tries closing lock screen from Recent Apps | Lock activity overrides back/home, uses kiosk mode window flags, sets status bar disabled, and restarts automatically if killed. |
| **Signing Key Compromise** | Malicious agent update pushed to fleet | Keystore is strictly isolated from repository; release builds only built on trusted machines with HSM/env-var credentials. |
| **Tenant Data Bleed** | Shop A attempts to view or lock Shop B's devices | `AbizTenantContext` enforces `shop_id` scoping at the MyBatis query level for all non-super-admin requests. |
| **Credit Ledger Over-Draft** | Concurrent enrollment attempts double-spending wallet balance | `SELECT ... FOR UPDATE` row locks on `abiz_wallets` + DB non-negative constraint (`balance >= 0`). |
| **JWT Tampering** | Client alters claims to elevate to `SUPER_ADMIN` | HMAC-SHA256 signature verification with server-only 256-bit secret. Claims derived strictly server-side upon authentication. |

---

## 2. Cryptographic Standards

- **Passwords**: BCrypt with work factor 12 (replacing legacy MD5).
- **JWT**: HMAC-SHA256 (256-bit entropy).
- **Agent Check-in Secrets**: SHA-256 hex digests with unique salts.
- **Android Signing Keystore**: RSA 4096-bit key with SHA-256 digest (`SHA256withRSA`).

---

## 3. Regulatory & Legal Considerations

- **Emergency Dialing**: In compliance with national and international telecom standards, dialing emergency numbers (e.g. 112, 911, 15) is **NEVER** blocked or restricted by the lock screen under any circumstances.
- **Customer Privacy**: Camera, microphone, and personal message spying features are intentionally **excluded** from ABIZ Locker. The system is strictly a financing collateral enforcement and device health management platform.
