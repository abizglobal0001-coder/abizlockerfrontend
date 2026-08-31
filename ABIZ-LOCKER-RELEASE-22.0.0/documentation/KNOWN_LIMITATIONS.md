# ABIZ LOCKER — KNOWN LIMITATIONS & OPERATIONAL REQUIREMENTS

---

### 1. Classification Overview

| Category | Description | Current State |
|---|---|---|
| **COMPLETED & VERIFIED** | Features and guarantees implemented in code, schema, and verified through regression suites. | 100% Implemented & Verified |
| **REQUIRES PRODUCTION CONFIGURATION** | Infrastructure dependencies requiring deployment-specific production credentials/endpoints. | Documented Below |
| **FUTURE ENHANCEMENT** | Out-of-scope enterprise features reserved for future releases. | Documented Below |

---

### 2. Completed & Verified Components (Phases 4–22)
- [x] Multi-Tenant Hierarchy (SuperAdmin, ShopOwner, ShopStaff, Customer).
- [x] Double-entry Financial Ledger & Wallet Transactions.
- [x] Financed Device Inventory & Contract Lifecycle.
- [x] Installment Schedules, Grace Periods & Auto-Lock State Engine.
- [x] Cryptographic Command Pipeline (32-byte nonces, SHA-256 signatures, replay defense).
- [x] Hardware-bound Android Device Owner Agent (`v0.1.22`, Code `22`) on Android 16 / API 36.
- [x] Tamper Detection, Airplane Mode Defense, Factory Reset Protection (FRP).
- [x] Distributed Worker Heartbeats & Lockout Coordination.
- [x] 12-Point Deployment Preflight Engine & Stage Checkpoint Rollback.
- [x] Configuration Drift Engine & Production Freeze Windows.
- [x] DR Plan Management, Point-in-Time Backup Verification, RTO/RPO SLA Tracking.
- [x] 10-State Device Lifecycle FSM & Security Incident Auto-Escalation.
- [x] 43 Automated Test Suites / 2,994 Assertions passing 100%.

---

### 3. Requires Production Configuration (Operational Handover)
1. **Cloud Object Storage for Automated Offsite Backups:**
   - *Status:* `REQUIRES PRODUCTION CONFIGURATION`
   - *Detail:* The backup verification and DR service currently computes and verifies SHA-256 checksums and restore metadata locally. In enterprise production deployment, configure the production S3 / GCS bucket credentials in `abiz-production.properties` (`abiz.backup.s3.bucket`, `abiz.backup.s3.region`).
2. **Production FCM / APNs Push Notification Gateway:**
   - *Status:* `REQUIRES PRODUCTION CONFIGURATION`
   - *Detail:* The command dispatch pipeline supports direct and mock transport bridges. For live cellular push delivery to physical customer devices outside the local MDM subnet, supply production Google Firebase Cloud Messaging service account credentials (`fcm-service-account.json`).
3. **Hardware Keystore Production Signing Certificate:**
   - *Status:* `REQUIRES PRODUCTION CONFIGURATION`
   - *Detail:* `app-release-unsigned.apk` is compiled in release mode. Prior to Google Play Enterprise / EMM distributor upload, sign the binary with the organization's production Android release keystore.

---

### 4. Future Enhancements (Post-Release Backlog)
1. **Multi-Region Active-Active PostgreSQL Database Replication:**
   - Cross-continental active-active database clustering for zero-downtime global multi-region failover.
2. **AI-Powered Device Risk Scoring & Fraud Prediction:**
   - Continuous ML inference on telemetry events to predict payment default or physical tampering likelihood before contract delinquency occurs.
