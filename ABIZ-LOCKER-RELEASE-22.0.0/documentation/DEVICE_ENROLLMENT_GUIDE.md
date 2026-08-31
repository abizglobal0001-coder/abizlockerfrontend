# DEVICE ENROLLMENT GUIDE
## Android Enterprise Device Owner Provisioning

---

### 1. QR Code / Zero-Touch Provisioning (Physical Devices)
For brand new out-of-the-box or freshly factory-reset customer devices:
1. Tap the setup welcome screen 6 times to trigger QR scanner.
2. Scan the enterprise provisioning QR code containing:
```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.mdmesh.agent.debug/com.mdmesh.agent.admin.AdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://download.abizlocker.com/apk/app-release-signed.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "X5SI2OIqkW8gCy/MUdg8K74ga24D+TjuWt8ALRQqMdw=",
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "server_url": "https://api.abizlocker.com",
    "shop_id": "<SHOP_UUID>"
  }
}
```

---

### 2. ADB Provisioning (Staging & Testing)
```bash
adb install -r app-release-signed.apk
adb shell dpm set-device-owner com.mdmesh.agent.debug/com.mdmesh.agent.admin.AdminReceiver
```
