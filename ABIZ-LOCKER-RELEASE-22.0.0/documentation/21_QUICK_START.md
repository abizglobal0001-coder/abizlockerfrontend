# 21 — QUICK START GUIDE
## 5-Minute Developer & Operator Quick Start

---

### 1. Start Database & Backend
```powershell
# 1. Start PostgreSQL 18 on port 3000 (abizlocker / abiz_dev_secure_pass_9281)
# 2. Run full cumulative regression suite
$java = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
$cp = "C:\Users\MNA\.m2\repository\javax\servlet\javax.servlet-api\3.0.1\javax.servlet-api-3.0.1.jar;C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server\target\launcher\WEB-INF\lib\*;C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server\target\classes;C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\common\target\classes;C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\jwt\target\classes"

& $java --source 11 -cp $cp "C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\scripts\RunFullCumulativeRegressionPhases4To22.java"
```

---

### 2. Verify Android Device Owner
```powershell
adb install -r C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\ABIZ-LOCKER-RELEASE-22.0.0\android\app-release-unsigned.apk
adb shell dpm set-device-owner com.mdmesh.agent.debug/com.mdmesh.agent.admin.AdminReceiver
adb shell dumpsys device_policy | findstr "admin="
```
