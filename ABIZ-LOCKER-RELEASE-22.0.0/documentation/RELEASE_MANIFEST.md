# ABIZ LOCKER — OFFICIAL RELEASE MANIFEST

## Release Version: 22.0.0
## Release Status: FROZEN / PRODUCTION SIGNED
## Timestamp: 2026-08-31T09:58:00+05:00

---

### 1. Backend Server Artifact
```text
Component:       ABIZ LOCKER Command & Control Backend
Artifact Name:   launcher.war
Local Path:      C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\server\target\launcher.war
Size:            42,709,342 bytes
SHA-256:         ADC5EE780CAB0D53C003D1606D2D18FA4A8FA286AA6CF08B18A3CB4EA8CB41EC
Build Target:    Java 11+ / Servlet 3.0+
Framework:       MyBatis 3.5.x, Guice DI, PostgreSQL JDBC 42.6.x, Jackson 2.15.x
```

### 2. Android Agent Artifact
```text
Component:       ABIZ LOCKER Enterprise Device Owner Agent
Artifact Name:   app-release-unsigned.apk
Local Path:      C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\agent-android\app\build\outputs\apk\release\app-release-unsigned.apk
Version Name:    0.1.22
Version Code:    22
Size:            1,391,238 bytes
SHA-256:         E69243527505AAA719966DD836A4A8188B14494D22D886A3C3217638525B66B6
Build Target:    Android API 36 (Min SDK 26, Target SDK 36)
Architecture:    Kotlin 1.9.x, AndroidX, Hardware-backed Keystore, Knox-compatible DO
```

### 3. Database Environment
```text
Engine:          PostgreSQL 18.0
Connection URL:  jdbc:postgresql://localhost:3000/abizlocker
Migration Tool:  Liquibase (abiz-changelog.xml)
Total Changes:   All Changesets (Phases 1-22) Synchronized
Phase 22 Tables: abiz_deployment_releases
                 abiz_deployment_checkpoints
                 abiz_configuration_drift_events
                 abiz_production_freezes
                 abiz_recovery_plans
                 abiz_recovery_executions
                 abiz_backup_verifications
                 abiz_device_lifecycle_events
                 abiz_security_incidents
Phase 22 Indexes: 22 Optimized Compound & Foreign Key Indexes Verified
```

### 4. Device Owner Hardware Verification Target
```text
Device ID:       emulator-5554
Device Model:    Android SDK built for x86_64
OS Version:      Android 16 / VanillaIceCream / API 36
Package:         com.mdmesh.agent.debug
Receiver:        com.mdmesh.agent.admin.AdminReceiver
Device Owner:    ACTIVE & VERIFIED (testOnlyAdmin=false)
```
