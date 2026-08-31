# 16 — MONITORING & OPERATIONAL HEALTH CHECKS
## Periodic Inspection Checklists (Daily, Weekly, Monthly)

---

### 1. Daily Health Checklist
- [ ] Verify API latency and error rate within SLO limits (`HealthService.getOperationalMetrics()`).
- [ ] Inspect background worker heartbeats in `abiz_worker_heartbeats`.
- [ ] Review active `CRITICAL` or `HIGH` security incidents in `abiz_security_incidents`.
- [ ] Confirm daily database backup completed and verified (`abiz_backup_verifications`).

---

### 2. Weekly Health Checklist
- [ ] Review configuration drift events in `abiz_configuration_drift_events`.
- [ ] Check fleet version compliance percentage (`FleetVersionComplianceService`).
- [ ] Audit multi-tenant access logs in `abiz_audit_logs`.
