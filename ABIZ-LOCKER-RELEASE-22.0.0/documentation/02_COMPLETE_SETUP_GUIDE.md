# 02 — COMPLETE SETUP GUIDE
## Development & Verification Environment Setup

---

### 1. Prerequisites
- **Operating System:** Windows 10/11 x64, Linux (Ubuntu 22.04 LTS), or macOS.
- **Java Development Kit (JDK):** OpenJDK 11 or 17 (JBR 17 recommended).
- **Android SDK:** API 36 Platform, Build Tools 34.0.0+, Platform Tools (adb).
- **Build System:** Apache Maven 3.8+, Gradle 8.2+.
- **Database Engine:** PostgreSQL 18.0 (configured on port `3000` for development).

---

### 2. Database Initialization
1. Start PostgreSQL 18 on port 3000.
2. Create database and user:
   ```sql
   CREATE USER abizlocker WITH PASSWORD 'abiz_dev_secure_pass_9281';
   CREATE DATABASE abizlocker OWNER abizlocker;
   GRANT ALL PRIVILEGES ON DATABASE abizlocker TO abizlocker;
   ```
3. Run Liquibase migrations:
   ```powershell
   cd C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server
   mvn liquibase:update
   ```

---

### 3. Backend Build & Packaging
Compile modules and package `launcher.war`:
```powershell
cd C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker
mvn clean package -DskipTests
```
Artifact generated at: `mdmesh/server/target/launcher.war` (Size: 42.7 MB).

---

### 4. Android Agent Build & Provisioning
1. Build release APK:
   ```powershell
   cd C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\agent-android
   .\gradlew assembleRelease
   ```
2. Install onto Android API 36 Emulator:
   ```powershell
   adb install -r app/build/outputs/apk/release/app-release-unsigned.apk
   ```
3. Provision Device Owner:
   ```powershell
   adb shell dpm set-device-owner com.mdmesh.agent.debug/com.mdmesh.agent.admin.AdminReceiver
   ```
4. Verify Device Owner status:
   ```powershell
   adb shell dumpsys device_policy | findstr "admin="
   ```
