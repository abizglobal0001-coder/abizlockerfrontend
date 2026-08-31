# 15 — TROUBLESHOOTING & DIAGNOSTICS GUIDE
## Diagnostic Matrix for Backend, Mobile Agent, and Security Operations

---

### 1. Backend & API Troubleshooting
- **Symptom:** API returns `401 Unauthorized`.
  - *Cause:* Expired JWT token or mismatched secret key.
  - *Resolution:* Re-authenticate via `/api/auth/login` and check system clock synchronization.
- **Symptom:** Database connection pool exhausted.
  - *Cause:* Leaked transactions or long-running audit queries.
  - *Resolution:* Inspect active connections in PostgreSQL (`SELECT * FROM pg_stat_activity;`) and verify connection timeouts.

---

### 2. Android Agent Troubleshooting
- **Symptom:** Device Owner provisioning fails (`dpm set-device-owner` error).
  - *Cause:* User accounts (Google/Email) already exist on device.
  - *Resolution:* Device Owner provisioning requires a freshly factory-reset device with zero accounts configured.
- **Symptom:** Device in `QUARANTINED` status.
  - *Cause:* Outdated agent version code (`< 22`).
  - *Resolution:* Distribute and install `v0.1.22` APK update.
