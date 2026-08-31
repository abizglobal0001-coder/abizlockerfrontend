# 07 — ANDROID AGENT OPERATIONS & MDM SPECIFICATION
## Enterprise Device Owner Architecture (v0.1.22 / Version Code 22)

---

### 1. Agent Architecture
The Android Agent (`com.mdmesh.agent.debug`) runs as a persistent Device Owner (DO) application leveraging Android Enterprise APIs (`DevicePolicyManager`):
- **Admin Receiver:** `com.mdmesh.agent.admin.AdminReceiver`
- **Keystore Service:** Generates hardware-backed EC keypairs in Android TEE / StrongBox.
- **Lock Overlay Engine:** Native window overlay blocking user interaction during `LOCKED` status while allowing emergency calls and payment portal access.
- **Self-Healing Guard:** Watches for package tamper, airplane mode abuse, and offline clock tampering.

---

### 2. Device Provisioning Options
1. **NFC / QR Code Provisioning (New / Factory Reset Devices):**
   - Format: Standard Android Enterprise JSON bundle with `ADMIN_PACKAGE_NAME` and cryptographic APK checksum.
2. **ADB Provisioning (Development / Staging):**
   ```bash
   adb install -r app-release-unsigned.apk
   adb shell dpm set-device-owner com.mdmesh.agent.debug/com.mdmesh.agent.admin.AdminReceiver
   ```

---

### 3. Offline & Tamper Defenses
- **Offline Lease Expiry:** If the device cannot connect to backend heartbeats within the configured TTL (default 7 days), it enters fail-safe lock mode.
- **Time Tamper Resistance:** Agent compares system clock against hardware RTC and monotonic boot time to prevent local date manipulation.
