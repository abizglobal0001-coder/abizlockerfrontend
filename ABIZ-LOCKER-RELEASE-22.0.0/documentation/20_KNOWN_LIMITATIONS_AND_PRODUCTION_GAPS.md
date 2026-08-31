# 20 — KNOWN LIMITATIONS & PRODUCTION GAPS
## Explicit Boundaries Between Engineering Completion and Production Infrastructure

---

### 1. Verified Engineering State
- **Backend Core:** 100% complete across all 22 phases.
- **Database Schema:** PostgreSQL 18 schema with 22 Phase 22 indexes fully operational.
- **Android Agent:** v0.1.22 (Code 22) verified on API 36 Emulator with active Device Owner.
- **Automated Regression:** 43 suites / 2,994 assertions passing 100%.

---

### 2. Operational Gaps Requiring Production Provisioning
1. **Production Keystore Signing:** `app-release-unsigned.apk` is compiled without private keys. Operations must sign with production enterprise keystore prior to EMM / Zero-Touch rollout.
2. **Cloud Backup Storage:** DR service verifies checksums locally; production S3 / GCS buckets must be configured in `abiz-production.properties`.
3. **Cellular Push Gateway:** Direct MDM bridge is active; production Google FCM service accounts are required for remote cellular wakeups.
