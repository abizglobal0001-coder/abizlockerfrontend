# 09 — DATABASE OPERATIONS & SCHEMA SPECIFICATION
## PostgreSQL 18 Architecture, Liquibase Migrations & Indexing

---

### 1. Schema Overview & Phase 22 Tables
All database migrations are managed deterministically via Liquibase (`abiz-changelog.xml`).

```text
abiz_shops ──────────────┐
                         ▼
abiz_customers ──► abiz_financed_devices ──► abiz_contracts ──► abiz_installments ──► abiz_payments
                         │
                         ├──► abiz_device_commands
                         ├──► abiz_device_licenses
                         ├──► abiz_device_lifecycle_events (Phase 22)
                         └──► abiz_security_incidents (Phase 22)

abiz_deployment_releases (Phase 22) ──► abiz_deployment_checkpoints (Phase 22)
abiz_configuration_drift_events (Phase 22)
abiz_production_freezes (Phase 22)
abiz_recovery_plans (Phase 22) ──► abiz_recovery_executions (Phase 22)
abiz_backup_verifications (Phase 22)
```

---

### 2. Key Phase 22 Table Definitions
1. `abiz_deployment_releases`: Immutable release registry with four-eyes approvals, SHA-256 hashes, and rollback tracking.
2. `abiz_deployment_checkpoints`: Granular pre/post-migration stage validation logs.
3. `abiz_configuration_drift_events`: Real-time configuration snapshot divergence alerts.
4. `abiz_production_freezes`: Scheduled freeze windows with audited emergency override logs.
5. `abiz_recovery_plans`: DR plan SLAs (RTO ≤ 300s, RPO ≤ 60s).
6. `abiz_recovery_executions`: Empirical disaster recovery execution records.
7. `abiz_backup_verifications`: Cryptographic backup checksum verifications.
8. `abiz_device_lifecycle_events`: Audit log for all 10 FSM state transitions.
9. `abiz_security_incidents`: Aggregated security events with SHA-256 fingerprinting and occurrence counters.
