# ABIZ LOCKER — Android Device Enrollment Guide

## 1. Enrollment Overview

ABIZ Locker requires devices to be provisioned as **Android Device Owner (DO)**.

```
+-------------------------------------------------------------------+
|                        ENROLLMENT LIFECYCLE                       |
|                                                                   |
|  [Shop Admin Console]           [Factory Fresh / Reset Phone]     |
|          |                                     |                  |
|  1. Reserve Credit (Ledger)                    |                  |
|  2. Generate Signed QR Token                   |                  |
|          |                                     |                  |
|          v                                     v                  |
|    Display QR on Web ------ Scan QR -----> Tap Welcome 6 Times    |
|                                                |                  |
|                                    3. Download & Install Agent    |
|                                    4. Set Device Owner (DO)       |
|                                    5. Provisioning Complete       |
|                                                |                  |
|                                    6. Check-in to MDMesh Core     |
|                                       (/agent/v1/device/status)   |
|                                                |                  |
|  7. Auto-Confirm Credit <---- Poll Status ----+                  |
|     (DEVICE_ACTIVATION_CONFIRM)                |                  |
|  8. Device state = ACTIVE                      v                  |
|     (Policy active, lock ready)     Locked to ABIZ Fleet          |
+-------------------------------------------------------------------+
```

---

## 2. Enrollment Methods

### Method A: Factory Reset QR Code (Primary / Customer Enrollment)
1. Device must be factory reset (out-of-box state).
2. Tap the "Welcome" setup screen **6 times** in the same spot.
3. The built-in Android QR code scanner launches.
4. Scan the QR code generated from the ABIZ Locker Web Console.
5. Android automatically connects to Wi-Fi, downloads the APK from the URL specified in the QR payload, verifies checksum, sets the app as Device Owner, and opens the agent.

### Method B: ADB Provisioning (Development & Testing)
If developing on an already configured device without factory resetting:
```powershell
# 1. Install debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 2. Set Device Owner via ADB
# (NOTE: Device must have NO accounts added in Settings -> Accounts!)
adb shell dpm set-device-owner com.mdmesh.agent.debug/com.mdmesh.agent.AdminReceiver
```

---

## 3. QR Code Payload Structure

The QR code encodes a JSON payload complying with Android Enterprise provisioning bundle:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.mdmesh.agent/com.mdmesh.agent.AdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME": "com.mdmesh.agent",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://<DOMAIN>/files/abiz-locker.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "<SHA-256-BASE64-OR-HEX>",
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "serverUrl": "https://<DOMAIN>",
    "enrollmentToken": "<SESSION_QR_TOKEN>",
    "shopId": "<SHOP_UUID>"
  },
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false
}
```

---

## 4. Activation Credit Safety Flow

1. **Step 1 — Reserve:**
   - Shop admin starts enrollment wizard for Customer X with Contract Y.
   - Backend calls `WalletService.reserveActivationCredit(shopId, sessionId, 1)`.
   - Transaction logged: `DEVICE_ACTIVATION_RESERVE`, amount: -1 credit.
   - If balance < 1: Enrollment blocked immediately (`INSUFFICIENT_CREDITS`).

2. **Step 2 — Handshake & Enrollment:**
   - Device scans QR, downloads agent, establishes Device Owner.
   - Agent contacts `/agent/v1/*` check-in endpoint with enrollment token.
   - MDMesh registers device, updates status to `ENROLLED`.

3. **Step 3 — Finalization:**
   - Backend detects successful first check-in within timeout (30 min).
   - Calls `WalletService.confirmActivation(shopId, sessionId)`.
   - Transaction logged: `DEVICE_ACTIVATION_CONFIRM`, amount: 0.
   - Device status changes to `ACTIVE`.

4. **Failure / Expiry:**
   - If enrollment token expires without first check-in or is cancelled:
   - Backend calls `WalletService.refundActivation(shopId, sessionId, 1)`.
   - Transaction logged: `DEVICE_ACTIVATION_REFUND`, amount: +1 credit.
   - Shop balance restored completely. Zero credit leakage.
