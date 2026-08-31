# 11 — DEPLOYMENT GOVERNANCE & RELEASE GATES RUNBOOK
## Four-Eyes Approval, Preflight Checklists & Atomic Rollbacks

---

### 1. Four-Eyes Peer Review Governance
To prevent insider threats or accidental misconfigurations, deployment releases require two distinct authorized actors:
1. `Initiator`: Registers release artifact (`launcher.war` / `app-release.apk`) in `DRAFT` status.
2. `Approver`: Reviews checksums, test logs, and approves release. *Self-approval is rejected by system policy.*

---

### 2. 12-Point Deterministic Preflight Engine
Rollout is blocked unless all 12 items evaluate to `true`:
1. `DATABASE_REACHABLE`: Live PostgreSQL connection established.
2. `SCHEMA_VERSION_VALID`: Liquibase migration state fully synchronized.
3. `WAR_HASH_VERIFIED`: Backend WAR SHA-256 matches approved release.
4. `APK_HASH_VERIFIED`: Android APK SHA-256 matches approved release.
5. `CONFIGURATION_APPROVED`: Active runtime parameters approved.
6. `WORKER_POOL_HEALTHY`: Background workers reporting heartbeats within 60s.
7. `SLO_HEALTH_ACCEPTABLE`: Platform API latency and error budgets healthy.
8. `NO_BLOCKING_CRITICAL_INCIDENTS`: Zero unmitigated CRITICAL security alerts.
9. `RECENT_BACKUP_VERIFIED`: Validated backup exists within RPO window.
10. `SECURITY_POSTURE_CLEAN`: Attestation and audit log integrity verified.
11. `FLEET_HEALTH_ACCEPTABLE`: Online fleet reporting normal status.
12. `ROLLBACK_ARTIFACT_AVAILABLE`: Validated prior stable release available for rollback.
