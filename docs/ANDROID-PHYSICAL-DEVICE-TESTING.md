# ABIZ LOCKER — Physical Android Device Testing Guide

## 1. Safety & Test Environment Guidelines

> **IMPORTANT WARNING:**
> Testing Device Owner (DO) capabilities on a personal smartphone will restrict or wipe accounts if not done carefully.
> ALWAYS use a dedicated development test phone, preferably running Android 10, 11, 12, 13, or 14.

---

## 2. Setting Up the Physical Device for CLI Testing

### Step 1: Enable USB Debugging
1. Go to **Settings > About Phone**.
2. Tap **Build Number** 7 times until "You are now a developer!" appears.
3. Go to **Settings > System > Developer Options**.
4. Enable **USB Debugging**.
5. Connect phone via USB to development PC.

### Step 2: Verify ADB Connection from CLI
```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
```
Output must show:
```
List of devices attached
<device_serial>    device
```
*(If unauthorized: look at phone screen and tap "Always allow from this computer".)*

---

## 3. Installing and Provisioning the Agent

### Step 3: Install APK
```powershell
& $adb install -r C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\agent-android\app\build\outputs\apk\debug\app-debug.apk
```

### Step 4: Provision as Device Owner via ADB
Ensure **NO Google or user accounts** exist on the device before executing:
```powershell
& $adb shell dpm set-device-owner com.mdmesh.agent.debug/com.mdmesh.agent.AdminReceiver
```
Success output:
```
Success: Device owner set to package ComponentInfo{com.mdmesh.agent.debug/com.mdmesh.agent.AdminReceiver}
```

---

## 4. Test Matrix: Verifying Locking Enforcement

| Test ID | Test Name | Action | Expected Result | Pass/Fail |
|---------|-----------|--------|-----------------|-----------|
| **TEST-01** | Basic Check-in | Connect device to Wi-Fi with tunnel URL configured | Agent checks in; device appears in MDMesh and ABIZ DB | |
| **TEST-02** | Lock Screen Invocation | Send `ABIZ_LOCK` command from backend | Device keyguard locks instantly; ABIZ lock screen takes over display | |
| **TEST-03** | Status Bar Block | Attempt to swipe down notification shade while locked | Status bar is disabled; quick settings inaccessible | |
| **TEST-04** | Home/Back Interception | Press Home / Back / Recent apps buttons while locked | Focus retained on ABIZ lock screen; desktop never revealed | |
| **TEST-05** | Reboot Persistence | Reboot phone with Power + Volume while locked | Upon boot completion, ABIZ lock screen displays immediately before home launcher | |
| **TEST-06** | Emergency Call Whitelist | Tap "Emergency Call" on lock screen | Phone dialer opens for emergency number (e.g. 112) only | |
| **TEST-07** | Shop Contact Call | Tap "Call Shop" button | Calls shop owner number directly from lock screen | |
| **TEST-08** | Unlock Handshake | Record payment in console, issuing `ABIZ_UNLOCK` | Lock screen dismisses gracefully; normal phone UI restored | |
| **TEST-09** | Safe Mode Resistance | Boot into Android Safe Mode | Safe boot disallowed by policy; normal boot forced or DO re-asserted | |
| **TEST-10** | Factory Reset Block | Check Settings > System > Reset Options | Factory reset option is greyed out with "Disabled by admin" message | |
